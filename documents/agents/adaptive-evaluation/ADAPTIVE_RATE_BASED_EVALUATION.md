# Adaptive Rate-Based Evaluation System

## Overview

This document describes the proposed **Adaptive Rate-Based Evaluation** algorithm for Eventara's real-time rule evaluation engine. The current implementation evaluates rules on every incoming event, which creates O(events × rules × redis) complexity - unsustainable at high throughput.

The adaptive approach dynamically adjusts evaluation frequency based on incoming event rate, optimizing for both **low-latency alerting** during high traffic and **resource conservation** during idle periods.

---

## Problem Statement

### Current Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                     Current: Per-Event Evaluation                 │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│   EventConsumer.ConsumeEvent()                                    │
│         │                                                         │
│         ▼                                                         │
│   realTimeRuleEvaluator.evaluateEvent(event)  ← Called per event │
│         │                                                         │
│         ▼                                                         │
│   for (AlertRule rule : rules) {              ← Loop 100-1k rules│
│       bucket = getFilteredMetrics(config);    ← Redis call       │
│       if (threshold crossed) fireAlert();                        │
│   }                                                              │
│                                                                   │
│   At 1000 events/sec × 500 rules = 500,000 Redis ops/sec 🔥      │
└──────────────────────────────────────────────────────────────────┘
```

### Issues

1. **Redis Overload**: Each event triggers `O(rules)` Redis reads
2. **Duplicate Work**: Same metrics re-fetched for every event
3. **No Batching**: Window-based metrics don't change per-event
4. **Wasted CPU**: Low-traffic periods still run expensive loops

---

## Proposed Solution: Adaptive Rate-Based Evaluation

### Core Concept

Instead of evaluating on every event, evaluate on a **dynamic tick interval** that adapts to the current event ingestion rate:

| Event Rate (per second) | Evaluation Interval | Rationale |
|-------------------------|---------------------|-----------|
| < 0.1 (< 5/min)         | 30 seconds          | Idle mode - conserve resources |
| 0.1 - 1 (5-50/min)      | 10 seconds          | Low traffic - responsive but efficient |
| 1 - 10 (50-500/min)     | 2 seconds           | Medium traffic - near real-time |
| 10 - 100 (500-5k/min)   | 500 milliseconds    | High traffic - fast response |
| > 100 (> 5k/min)        | 100 milliseconds    | Burst mode - maximum responsiveness |

### Complexity Reduction

```
┌──────────────────────────────────────────────────────────────────┐
│                  Proposed: Adaptive Tick Evaluation               │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│   EventConsumer.ConsumeEvent()                                    │
│         │                                                         │
│         ▼                                                         │
│   eventCounter.incrementAndGet();   ← O(1) atomic operation      │
│   dirtyFlag.set(true);              ← O(1)                       │
│                                                                   │
│   ─────────────────────────────────────────────────────────────  │
│                                                                   │
│   @Scheduled (dynamic interval)                                   │
│         │                                                         │
│         ▼                                                         │
│   if (!dirty) return;               ← Skip if no new events      │
│   snapshot = captureMetricsOnce();  ← 1 Redis call per tick     │
│   for (AlertRule rule : rules) {                                 │
│       evaluateAgainstSnapshot(rule, snapshot);  ← No Redis!     │
│   }                                                              │
│   adjustInterval(eventRate);        ← Adapt for next tick       │
│                                                                   │
│   At 1000 events/sec = ~20 Redis ops/sec 🚀                     │
└──────────────────────────────────────────────────────────────────┘
```

---

## Implementation Design

### 1. Event Rate Monitor

Track events per second using an atomic counter with sliding window:

```java
@Component
public class EventRateMonitor {
    
    private final AtomicLong eventCounter = new AtomicLong(0);
    private volatile long lastSampleTime = System.currentTimeMillis();
    private volatile double currentEps = 0.0;  // Events per second
    
    // Exponential moving average factor (0.3 = 30% new, 70% old)
    private static final double EMA_ALPHA = 0.3;
    
    /**
     * Called on every event - O(1) operation
     */
    public void recordEvent() {
        eventCounter.incrementAndGet();
    }
    
    /**
     * Calculate EPS and update the moving average
     * Called periodically (e.g., every second)
     */
    public double sampleAndGetEps() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastSampleTime;
        
        if (elapsed < 100) return currentEps; // Min sample interval
        
        long events = eventCounter.getAndSet(0);
        double instantEps = (events * 1000.0) / elapsed;
        
        // Exponential moving average for smooth transitions
        currentEps = (EMA_ALPHA * instantEps) + ((1 - EMA_ALPHA) * currentEps);
        lastSampleTime = now;
        
        return currentEps;
    }
    
    public double getCurrentEps() {
        return currentEps;
    }
}
```

### 2. Adaptive Evaluation Scheduler

Dynamic scheduling based on event rate:

```java
@Service
@Slf4j
public class AdaptiveRuleEvaluator {
    
    private final EventRateMonitor rateMonitor;
    private final RuleRepository ruleRepository;
    private final RedisMetricsService redisMetrics;
    private final AlertTriggerHandler alertHandler;
    
    private final AtomicBoolean dirtyFlag = new AtomicBoolean(false);
    private volatile long currentIntervalMs = 10_000; // Start at 10s
    private volatile long lastEvaluationTime = System.currentTimeMillis();
    
    // Configurable thresholds
    private final EvaluationConfig config;
    
    /**
     * Called from EventConsumer on every event - O(1)
     */
    public void onEventIngested() {
        rateMonitor.recordEvent();
        dirtyFlag.set(true);
    }
    
    /**
     * Scheduled check - runs frequently but does minimal work
     */
    @Scheduled(fixedRate = 100) // Check every 100ms
    public void checkAndEvaluate() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastEvaluationTime;
        
        // Not time yet, or no new events
        if (elapsed < currentIntervalMs || !dirtyFlag.get()) {
            return;
        }
        
        // Reset and evaluate
        dirtyFlag.set(false);
        lastEvaluationTime = now;
        
        try {
            evaluateAllRules();
        } finally {
            // Adjust interval based on current event rate
            double eps = rateMonitor.sampleAndGetEps();
            currentIntervalMs = calculateInterval(eps);
            
            log.debug("Adaptive interval adjusted: EPS={}, interval={}ms", 
                      eps, currentIntervalMs);
        }
    }
    
    private void evaluateAllRules() {
        List<AlertRule> rules = getCachedActiveRules();
        
        // Group rules by evaluation key to share Redis reads
        Map<EvaluationKey, List<AlertRule>> ruleGroups = groupByEvaluationKey(rules);
        
        for (Map.Entry<EvaluationKey, List<AlertRule>> entry : ruleGroups.entrySet()) {
            // ONE Redis call per group
            MetricsBucket bucket = fetchMetrics(entry.getKey());
            
            for (AlertRule rule : entry.getValue()) {
                evaluateRuleAgainstBucket(rule, bucket);
            }
        }
    }
    
    /**
     * Calculate evaluation interval based on events per second
     */
    private long calculateInterval(double eventsPerSecond) {
        if (eventsPerSecond < config.getIdleThreshold()) {
            return config.getIdleIntervalMs();      // 30s for idle
        }
        if (eventsPerSecond < config.getLowThreshold()) {
            return config.getLowIntervalMs();       // 10s for low
        }
        if (eventsPerSecond < config.getMediumThreshold()) {
            return config.getMediumIntervalMs();    // 2s for medium
        }
        if (eventsPerSecond < config.getHighThreshold()) {
            return config.getHighIntervalMs();      // 500ms for high
        }
        return config.getBurstIntervalMs();         // 100ms for burst
    }
}
```

### 3. Evaluation Configuration

Externalized configuration for tuning:

```java
@Component
@ConfigurationProperties(prefix = "eventara.evaluation.adaptive")
@Data
public class EvaluationConfig {
    
    // Event rate thresholds (events per second)
    private double idleThreshold = 0.1;      // < 5/min
    private double lowThreshold = 1.0;       // < 50/min
    private double mediumThreshold = 10.0;   // < 500/min
    private double highThreshold = 100.0;    // < 5000/min
    
    // Corresponding intervals (milliseconds)
    private long idleIntervalMs = 30_000;    // 30 seconds
    private long lowIntervalMs = 10_000;     // 10 seconds
    private long mediumIntervalMs = 2_000;   // 2 seconds
    private long highIntervalMs = 500;       // 500ms
    private long burstIntervalMs = 100;      // 100ms
    
    // Feature flag
    private boolean enabled = true;
    
    // Minimum events before evaluation (prevents false alerts)
    private int minEventsToEvaluate = 1;
}
```

### 4. Rule Grouping for Metrics Sharing

Group rules by evaluation key to minimize Redis calls:

```java
/**
 * Key that determines which rules can share the same metrics fetch
 */
@Value
public class EvaluationKey {
    int windowMinutes;
    List<String> sourceFilter;    // null = global
    List<String> eventTypeFilter; // null = all types
    
    public static EvaluationKey fromRule(AlertRule rule) {
        Map<String, Object> config = rule.getRuleConfig();
        return new EvaluationKey(
            parseIntOrDefault(config.get("timeWindowMinutes"), 15),
            (List<String>) config.get("sourceFilter"),
            (List<String>) config.get("eventTypeFilter")
        );
    }
}

// Usage in evaluation
Map<EvaluationKey, List<AlertRule>> groups = rules.stream()
    .collect(Collectors.groupingBy(EvaluationKey::fromRule));

// Result: Rules with same window+filters share ONE Redis call
// e.g., 500 rules might reduce to 20-50 unique Redis calls
```

---

## Migration Path

### Phase 1: Add Alongside Existing (1-2 days)

1. Create `AdaptiveRuleEvaluator` as new component
2. Add feature flag `eventara.evaluation.adaptive.enabled` (set to `false` to disable)
3. Keep `RealTimeRuleEvaluator.evaluateEvent()` as fallback
4. Test both paths in parallel

### Phase 2: Gradual Rollout (1 week)

1. Enable for 10% of traffic (canary)
2. Monitor latency, Redis ops, alert accuracy
3. Tune thresholds based on real data
4. Increase to 50%, then 100%

### Phase 3: Cleanup (1 day)

1. Remove per-event evaluation path
2. Simplify `EventConsumer` to just increment counter
3. Document final configuration

---

## Metrics & Observability

### Key Metrics to Track

```java
// Expose via Micrometer/Prometheus
@Gauge("eventara.evaluation.current_interval_ms")
public long getCurrentIntervalMs() { return currentIntervalMs; }

@Gauge("eventara.evaluation.current_eps")  
public double getCurrentEps() { return rateMonitor.getCurrentEps(); }

@Counter("eventara.evaluation.ticks_total")
public void recordTick() { /* ... */ }

@Histogram("eventara.evaluation.tick_duration_ms")
public void recordTickDuration(long durationMs) { /* ... */ }
```

### Dashboard Queries

```promql
# Current evaluation interval
eventara_evaluation_current_interval_ms

# Events per second trend
rate(eventara_events_total[1m])

# Evaluation tick frequency
rate(eventara_evaluation_ticks_total[1m])

# Redis ops reduction
rate(redis_commands_total{command="HGETALL"}[1m])
```

---

## Expected Benefits

| Metric | Before (Per-Event) | After (Adaptive) | Improvement |
|--------|-------------------|------------------|-------------|
| Redis ops/sec @ 1000 EPS | 500,000 | ~50 | **10,000x** |
| CPU usage (evaluator) | High, constant | Scales with traffic | **Variable** |
| Alert latency @ idle | Instant | 30s max | Acceptable |
| Alert latency @ burst | Instant | 100ms max | Acceptable |
| Memory (per instance) | Low | Low + rate buffer | Minimal |

---

## Configuration Example

```yaml
# application.yml
eventara:
  evaluation:
    adaptive:
      enabled: true
      
      # Event rate thresholds (events per second)
      idle-threshold: 0.1       # < 5 events/min
      low-threshold: 1.0        # < 50 events/min  
      medium-threshold: 10.0    # < 500 events/min
      high-threshold: 100.0     # < 5000 events/min
      
      # Evaluation intervals (milliseconds)
      idle-interval-ms: 30000   # 30 seconds
      low-interval-ms: 10000    # 10 seconds
      medium-interval-ms: 2000  # 2 seconds
      high-interval-ms: 500     # 500ms
      burst-interval-ms: 100    # 100ms
      
      # Smoothing factor for EPS calculation
      ema-alpha: 0.3
```

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Delayed alerts during idle | Set max interval cap (30s) that's business-acceptable |
| Flash traffic spikes | Quick ramp-up (100ms poll to detect, immediate switch to burst mode) |
| Metrics stale during evaluation | Use dirty flag to skip redundant evaluations |
| Configuration complexity | Sensible defaults, override only when needed |

---

## References

- Current implementation: `RealTimeRuleEvaluator.java`
- Redis metrics: `RedisMetricsService.java`
- Event ingestion: `EventConsumer.java`
- Rule storage: `AlertRule.java` (ruleConfig JSONB)

---

## Implementation Checklist

- [ ] Create `EventRateMonitor` component
- [ ] Create `EvaluationConfig` configuration class
- [ ] Create `EvaluationKey` for rule grouping
- [ ] Create `AdaptiveRuleEvaluator` service
- [ ] Update `EventConsumer` to call `onEventIngested()`
- [ ] Add Micrometer metrics
- [ ] Add feature flag configuration
- [ ] Write integration tests
- [ ] Load test with realistic event patterns
- [ ] Document runbook for tuning thresholds

---

*Document Version: 1.0*  
*Created: 2026-02-04*  
*Author: AI Agent Collaboration*
