# Adaptive Rate-Based Evaluation - Implementation Plan

## Overview

This plan transforms Eventara's rule evaluation from **per-event** to **adaptive tick-based**, reducing Redis operations from O(events × rules) to O(rules/tick).

---

## Current Codebase Reference

| Component | File Path | Purpose |
|-----------|-----------|---------|
| Evaluator | `src/main/java/com/eventara/rule/evaluation/RealTimeRuleEvaluator.java` | Current per-event evaluation |
| State | `src/main/java/com/eventara/rule/evaluation/ThresholdState.java` | In-memory fallback state |
| Consumer | `src/main/java/com/eventara/ingestion/kafka/EventConsumer.java` | Kafka event ingestion |
| Redis Metrics | `src/main/java/com/eventara/metrics/service/RedisMetricsService.java` | Metrics from Redis |
| Config | `src/main/java/com/eventara/metrics/config/MetricsProperties.java` | Metrics configuration pattern |
| Properties | `src/main/resources/application.properties` | Application configuration |

---

## Phase 1: Configuration Infrastructure

**Goal**: Add configuration classes following existing `MetricsProperties` pattern.

**Duration**: 30 minutes

### 1.1 Create AdaptiveEvaluationProperties.java

**File**: `src/main/java/com/eventara/rule/evaluation/config/AdaptiveEvaluationProperties.java`

```java
package com.eventara.rule.evaluation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "eventara.evaluation.adaptive")
public class AdaptiveEvaluationProperties {

    private boolean enabled = false;
    private Thresholds thresholds = new Thresholds();
    private Intervals intervals = new Intervals();

    public static class Thresholds {
        private double idle = 0.1;       // < 5 events/min
        private double low = 1.0;        // < 50 events/min
        private double medium = 10.0;    // < 500 events/min
        private double high = 100.0;     // < 5000 events/min
        
        // Getters and setters
        public double getIdle() { return idle; }
        public void setIdle(double idle) { this.idle = idle; }
        public double getLow() { return low; }
        public void setLow(double low) { this.low = low; }
        public double getMedium() { return medium; }
        public void setMedium(double medium) { this.medium = medium; }
        public double getHigh() { return high; }
        public void setHigh(double high) { this.high = high; }
    }

    public static class Intervals {
        private long idleMs = 30000;     // 30 seconds
        private long lowMs = 10000;      // 10 seconds
        private long mediumMs = 2000;    // 2 seconds
        private long highMs = 500;       // 500ms
        private long burstMs = 100;      // 100ms
        
        // Getters and setters
        public long getIdleMs() { return idleMs; }
        public void setIdleMs(long idleMs) { this.idleMs = idleMs; }
        public long getLowMs() { return lowMs; }
        public void setLowMs(long lowMs) { this.lowMs = lowMs; }
        public long getMediumMs() { return mediumMs; }
        public void setMediumMs(long mediumMs) { this.mediumMs = mediumMs; }
        public long getHighMs() { return highMs; }
        public void setHighMs(long highMs) { this.highMs = highMs; }
        public long getBurstMs() { return burstMs; }
        public void setBurstMs(long burstMs) { this.burstMs = burstMs; }
    }

    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Thresholds getThresholds() { return thresholds; }
    public void setThresholds(Thresholds thresholds) { this.thresholds = thresholds; }
    public Intervals getIntervals() { return intervals; }
    public void setIntervals(Intervals intervals) { this.intervals = intervals; }
}
```

### 1.2 Add Configuration to application.properties

**Append to**: `src/main/resources/application.properties`

```properties
# =========================
# Adaptive Evaluation Configuration
# =========================
eventara.evaluation.adaptive.enabled=${EVENTARA_ADAPTIVE_ENABLED:true}

# Event rate thresholds (events per second)
eventara.evaluation.adaptive.thresholds.idle=0.1
eventara.evaluation.adaptive.thresholds.low=1.0
eventara.evaluation.adaptive.thresholds.medium=10.0
eventara.evaluation.adaptive.thresholds.high=100.0

# Evaluation intervals (milliseconds)
eventara.evaluation.adaptive.intervals.idle-ms=30000
eventara.evaluation.adaptive.intervals.low-ms=10000
eventara.evaluation.adaptive.intervals.medium-ms=2000
eventara.evaluation.adaptive.intervals.high-ms=500
eventara.evaluation.adaptive.intervals.burst-ms=100
```

---

## Phase 2: Event Rate Monitor

**Goal**: Create lightweight event counter with EPS (events per second) calculation.

**Duration**: 45 minutes

### 2.1 Create EventRateMonitor.java

**File**: `src/main/java/com/eventara/rule/evaluation/EventRateMonitor.java`

```java
package com.eventara.rule.evaluation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class EventRateMonitor {

    private final AtomicLong eventCounter = new AtomicLong(0);
    private final AtomicLong errorCounter = new AtomicLong(0);
    private volatile long lastSampleTime = System.currentTimeMillis();
    private volatile double currentEps = 0.0;
    
    // Exponential moving average factor
    private static final double EMA_ALPHA = 0.3;
    
    /**
     * Record an event - O(1) atomic operation.
     * Called from EventConsumer on every event.
     */
    public void recordEvent(boolean isError) {
        eventCounter.incrementAndGet();
        if (isError) {
            errorCounter.incrementAndGet();
        }
    }
    
    /**
     * Sample the event rate and reset counters.
     * Returns smoothed EPS using exponential moving average.
     */
    public double sampleAndGetEps() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastSampleTime;
        
        if (elapsed < 100) {
            return currentEps; // Minimum sample interval
        }
        
        long events = eventCounter.getAndSet(0);
        errorCounter.set(0); // Reset errors too
        
        double instantEps = (events * 1000.0) / elapsed;
        
        // Smooth with EMA to prevent rapid oscillation
        currentEps = (EMA_ALPHA * instantEps) + ((1 - EMA_ALPHA) * currentEps);
        lastSampleTime = now;
        
        log.debug("Event rate sampled: instantEps={}, smoothedEps={}", 
                  instantEps, currentEps);
        
        return currentEps;
    }
    
    public double getCurrentEps() {
        return currentEps;
    }
    
    public long getPendingEventCount() {
        return eventCounter.get();
    }
}
```

---

## Phase 3: Evaluation Key for Rule Grouping

**Goal**: Group rules by window/filter to share Redis calls.

**Duration**: 30 minutes

### 3.1 Create EvaluationKey.java

**File**: `src/main/java/com/eventara/rule/evaluation/EvaluationKey.java`

```java
package com.eventara.rule.evaluation;

import com.eventara.rule.entity.AlertRule;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Value
public class EvaluationKey {
    
    int windowMinutes;
    List<String> sourceFilter;
    List<String> eventTypeFilter;
    
    @SuppressWarnings("unchecked")
    public static EvaluationKey fromRule(AlertRule rule, int defaultWindow) {
        Map<String, Object> config = rule.getRuleConfig();
        if (config == null) {
            return new EvaluationKey(defaultWindow, null, null);
        }
        
        int window = defaultWindow;
        if (config.containsKey("timeWindowMinutes")) {
            Object val = config.get("timeWindowMinutes");
            if (val instanceof Number) {
                window = ((Number) val).intValue();
            }
        }
        
        List<String> sources = (List<String>) config.get("sourceFilter");
        List<String> types = (List<String>) config.get("eventTypeFilter");
        
        return new EvaluationKey(window, sources, types);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvaluationKey that = (EvaluationKey) o;
        return windowMinutes == that.windowMinutes &&
               Objects.equals(sourceFilter, that.sourceFilter) &&
               Objects.equals(eventTypeFilter, that.eventTypeFilter);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(windowMinutes, sourceFilter, eventTypeFilter);
    }
}
```

---

## Phase 4: Adaptive Rule Evaluator

**Goal**: Main evaluation service with dynamic tick scheduling.

**Duration**: 2 hours

### 4.1 Create AdaptiveRuleEvaluator.java

**File**: `src/main/java/com/eventara/rule/evaluation/AdaptiveRuleEvaluator.java`

```java
package com.eventara.rule.evaluation;

import com.eventara.alert.service.AlertTriggerHandler;
import com.eventara.metrics.config.MetricsProperties;
import com.eventara.metrics.model.MetricsBucket;
import com.eventara.metrics.service.RedisMetricsService;
import com.eventara.rule.entity.AlertRule;
import com.eventara.rule.enums.RuleStatus;
import com.eventara.rule.enums.RuleType;
import com.eventara.rule.evaluation.config.AdaptiveEvaluationProperties;
import com.eventara.rule.repository.RuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdaptiveRuleEvaluator {

    private final AdaptiveEvaluationProperties config;
    private final EventRateMonitor rateMonitor;
    private final RuleRepository ruleRepository;
    private final RedisMetricsService redisMetrics;
    private final AlertTriggerHandler alertHandler;
    private final MetricsProperties metricsProperties;
    private final StringRedisTemplate stringRedisTemplate;
    
    // State
    private final AtomicBoolean dirtyFlag = new AtomicBoolean(false);
    private volatile long currentIntervalMs = 10_000;
    private volatile long lastEvaluationTime = System.currentTimeMillis();
    
    // Rule cache (same pattern as RealTimeRuleEvaluator)
    private volatile List<AlertRule> cachedRules;
    private volatile long lastRuleRefresh = 0;
    private static final long RULE_CACHE_TTL_MS = 60_000;
    
    // Cooldown prefix
    private static final String COOLDOWN_PREFIX = "eventara:rule:cooldown:";
    
    @PostConstruct
    public void init() {
        if (config.isEnabled()) {
            refreshRuleCache();
            log.info("AdaptiveRuleEvaluator initialized: {} rules, starting interval={}ms",
                    cachedRules != null ? cachedRules.size() : 0, currentIntervalMs);
        } else {
            log.info("AdaptiveRuleEvaluator is DISABLED");
        }
    }
    
    /**
     * Called from EventConsumer - just marks dirty, O(1).
     */
    public void onEventIngested(boolean isError) {
        if (!config.isEnabled()) return;
        
        rateMonitor.recordEvent(isError);
        dirtyFlag.set(true);
    }
    
    /**
     * High-frequency check - minimal work if not time yet.
     */
    @Scheduled(fixedRate = 100)
    public void checkAndEvaluate() {
        if (!config.isEnabled()) return;
        
        long now = System.currentTimeMillis();
        long elapsed = now - lastEvaluationTime;
        
        // Not time yet OR no new events
        if (elapsed < currentIntervalMs || !dirtyFlag.get()) {
            return;
        }
        
        // Reset and evaluate
        dirtyFlag.set(false);
        lastEvaluationTime = now;
        
        try {
            evaluateAllRulesGrouped();
        } finally {
            // Adjust interval based on current rate
            double eps = rateMonitor.sampleAndGetEps();
            currentIntervalMs = calculateInterval(eps);
            
            log.debug("Adaptive tick: EPS={}, nextInterval={}ms", eps, currentIntervalMs);
        }
    }
    
    private void evaluateAllRulesGrouped() {
        List<AlertRule> rules = getActiveThresholdRules();
        if (rules.isEmpty()) return;
        
        int defaultWindow = metricsProperties.getBucket().getRedisRetentionMinutes();
        
        // Group rules by evaluation key
        Map<EvaluationKey, List<AlertRule>> groups = rules.stream()
            .collect(Collectors.groupingBy(r -> EvaluationKey.fromRule(r, defaultWindow)));
        
        log.debug("Evaluating {} rules in {} groups", rules.size(), groups.size());
        
        for (Map.Entry<EvaluationKey, List<AlertRule>> entry : groups.entrySet()) {
            EvaluationKey key = entry.getKey();
            List<AlertRule> groupRules = entry.getValue();
            
            // ONE Redis call per group
            MetricsBucket bucket = fetchMetricsForKey(key);
            
            for (AlertRule rule : groupRules) {
                evaluateRuleAgainstBucket(rule, bucket, key.getWindowMinutes());
            }
        }
    }
    
    private MetricsBucket fetchMetricsForKey(EvaluationKey key) {
        if (key.getSourceFilter() != null && !key.getSourceFilter().isEmpty()) {
            if (key.getSourceFilter().size() == 1) {
                return redisMetrics.getMetricsForSource(
                    key.getSourceFilter().get(0), key.getWindowMinutes());
            }
            return redisMetrics.getMetricsForSources(
                key.getSourceFilter(), key.getWindowMinutes());
        }
        
        if (key.getEventTypeFilter() != null && !key.getEventTypeFilter().isEmpty() 
                && key.getEventTypeFilter().size() == 1) {
            return redisMetrics.getMetricsForEventType(
                key.getEventTypeFilter().get(0), key.getWindowMinutes());
        }
        
        return redisMetrics.getMetricsLastMinutes(key.getWindowMinutes());
    }
    
    @SuppressWarnings("unchecked")
    private void evaluateRuleAgainstBucket(AlertRule rule, MetricsBucket bucket, int windowMinutes) {
        Map<String, Object> config = rule.getRuleConfig();
        if (config == null) return;
        
        // Skip composite/virtual - they need special handling
        if (config.containsKey("conditions")) return;
        
        String metricType = (String) config.get("metricType");
        String condition = (String) config.get("condition");
        Object thresholdObj = config.get("thresholdValue");
        int cooldownMinutes = parseIntOrDefault(config.get("cooldownMinutes"), 5);
        
        if (metricType == null || condition == null || thresholdObj == null) return;
        
        double threshold = parseDouble(thresholdObj);
        double currentValue = getMetricValue(metricType, bucket, windowMinutes);
        
        boolean crossed = isThresholdCrossed(condition, currentValue, threshold);
        
        if (crossed && !isInCooldown(rule.getId(), cooldownMinutes)) {
            fireAlert(rule, currentValue, threshold);
            setCooldown(rule.getId(), cooldownMinutes);
            log.info("Rule '{}' fired via adaptive eval: {} {} {} (current={})",
                    rule.getName(), metricType, condition, threshold, currentValue);
        }
    }
    
    private long calculateInterval(double eps) {
        var thresholds = config.getThresholds();
        var intervals = config.getIntervals();
        
        if (eps < thresholds.getIdle()) return intervals.getIdleMs();
        if (eps < thresholds.getLow()) return intervals.getLowMs();
        if (eps < thresholds.getMedium()) return intervals.getMediumMs();
        if (eps < thresholds.getHigh()) return intervals.getHighMs();
        return intervals.getBurstMs();
    }
    
    // === Helper methods (copied from RealTimeRuleEvaluator) ===
    
    private double getMetricValue(String metricType, MetricsBucket bucket, int windowMinutes) {
        switch (metricType) {
            case "ERROR_RATE": return bucket.getErrorRate();
            case "TOTAL_ERRORS": return bucket.getTotalErrors();
            case "AVG_LATENCY": return bucket.getAvgLatency();
            case "TOTAL_EVENTS": return bucket.getTotalEvents();
            case "EVENTS_PER_MINUTE": return bucket.getTotalEvents() / (double) Math.max(1, windowMinutes);
            case "P95_LATENCY": return bucket.getLatencyP95() != null ? bucket.getLatencyP95() : 0;
            case "P99_LATENCY": return bucket.getLatencyP99() != null ? bucket.getLatencyP99() : 0;
            default: return 0;
        }
    }
    
    private boolean isThresholdCrossed(String condition, double current, double threshold) {
        switch (condition) {
            case "GREATER_THAN": return current > threshold;
            case "GREATER_THAN_OR_EQUAL": return current >= threshold;
            case "LESS_THAN": return current < threshold;
            case "LESS_THAN_OR_EQUAL": return current <= threshold;
            case "EQUALS": return Math.abs(current - threshold) < 0.0001;
            default: return false;
        }
    }
    
    private boolean isInCooldown(Long ruleId, int cooldownMinutes) {
        String key = COOLDOWN_PREFIX + ruleId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }
    
    private void setCooldown(Long ruleId, int cooldownMinutes) {
        String key = COOLDOWN_PREFIX + ruleId;
        stringRedisTemplate.opsForValue().set(key, 
            String.valueOf(System.currentTimeMillis()),
            java.time.Duration.ofMinutes(cooldownMinutes));
    }
    
    private void fireAlert(AlertRule rule, double currentValue, double threshold) {
        alertHandler.handleThresholdAlert(
            rule.getId(), rule.getName(), rule.getSeverity().name(),
            threshold, currentValue);
    }
    
    private List<AlertRule> getActiveThresholdRules() {
        long now = System.currentTimeMillis();
        if (cachedRules == null || (now - lastRuleRefresh) > RULE_CACHE_TTL_MS) {
            refreshRuleCache();
        }
        return cachedRules;
    }
    
    public void refreshRuleCache() {
        cachedRules = ruleRepository.findByRuleTypeAndStatus(RuleType.THRESHOLD, RuleStatus.ACTIVE);
        lastRuleRefresh = System.currentTimeMillis();
    }
    
    public void invalidateCache() {
        cachedRules = null;
        refreshRuleCache();
    }
    
    private double parseDouble(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }
    
    private int parseIntOrDefault(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); } 
        catch (Exception e) { return defaultValue; }
    }
    
    // Metrics for monitoring
    public long getCurrentIntervalMs() { return currentIntervalMs; }
    public double getCurrentEps() { return rateMonitor.getCurrentEps(); }
}
```

---

## Phase 5: Integration with EventConsumer

**Goal**: Update EventConsumer to use adaptive evaluator.

**Duration**: 30 minutes

### 5.1 Modify EventConsumer.java

**File**: `src/main/java/com/eventara/ingestion/kafka/EventConsumer.java`

**Changes**:

```java
// Add import
import com.eventara.rule.evaluation.AdaptiveRuleEvaluator;
import com.eventara.rule.evaluation.config.AdaptiveEvaluationProperties;

// Add autowired field
@Autowired
private AdaptiveRuleEvaluator adaptiveRuleEvaluator;

@Autowired
private AdaptiveEvaluationProperties adaptiveConfig;

// In ConsumeEvent method, replace line 99:
// OLD:
// realTimeRuleEvaluator.evaluateEvent(eventDto);

// NEW:
if (adaptiveConfig.isEnabled()) {
    // O(1) - just increment counter and set dirty flag
    adaptiveRuleEvaluator.onEventIngested(eventDto.isError());
} else {
    // Legacy per-event evaluation
    realTimeRuleEvaluator.evaluateEvent(eventDto);
}
```

---

## Phase 6: Enable Scheduling

**Goal**: Ensure @Scheduled annotations work.

**Duration**: 15 minutes

### 6.1 Verify EventaraApplication.java

**File**: `src/main/java/com/eventara/EventaraApplication.java`

Ensure `@EnableScheduling` is present:

```java
package com.eventara;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // <-- Required for @Scheduled to work
public class EventaraApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventaraApplication.class, args);
    }
}
```

---

## Phase 7: Testing

**Goal**: Verify the implementation works correctly.

**Duration**: 1 hour

### 7.1 Unit Tests

**File**: `src/test/java/com/eventara/rule/evaluation/AdaptiveRuleEvaluatorTest.java`

Test cases:
1. Event rate monitor correctly calculates EPS
2. Interval calculation returns correct values for different EPS
3. Rule grouping creates correct EvaluationKeys
4. Metrics are fetched once per group
5. Cooldown works correctly

### 7.2 Integration Test

**File**: `tests/test_adaptive_evaluation.py` (Python test like existing tests)

Test:
1. Create rules with different windows/filters
2. Send events at varying rates
3. Verify alerts fire correctly
4. Verify Redis call count is reduced

---

## Phase 8: Monitoring & Observability

**Goal**: Add metrics for dashboard visibility.

**Duration**: 30 minutes

### 8.1 Add Micrometer Metrics (Optional)

Create a metrics exporter:

```java
@Component
@RequiredArgsConstructor
public class AdaptiveEvaluationMetrics {
    
    private final AdaptiveRuleEvaluator evaluator;
    private final MeterRegistry registry;
    
    @PostConstruct
    public void init() {
        Gauge.builder("eventara.evaluation.interval_ms", evaluator::getCurrentIntervalMs)
            .register(registry);
        Gauge.builder("eventara.evaluation.current_eps", evaluator::getCurrentEps)
            .register(registry);
    }
}
```

---

## Implementation Checklist

### Phase 1: Configuration ✅
- [ ] Create `AdaptiveEvaluationProperties.java`
- [ ] Add properties to `application.properties`

### Phase 2: Event Rate Monitor ✅
- [ ] Create `EventRateMonitor.java`

### Phase 3: Evaluation Key ✅
- [ ] Create `EvaluationKey.java`

### Phase 4: Adaptive Evaluator ✅
- [ ] Create `AdaptiveRuleEvaluator.java`

### Phase 5: Integration ✅
- [ ] Modify `EventConsumer.java`
- [ ] Add feature flag check

### Phase 6: Scheduling ✅
- [ ] Verify `@EnableScheduling`

### Phase 7: Testing ✅
- [ ] Unit tests
- [ ] Integration tests

### Phase 8: Monitoring ✅
- [ ] Add Micrometer metrics (optional)

---

## Rollout Strategy

1. **Development**: Set `eventara.evaluation.adaptive.enabled=true` locally
2. **Staging**: Test with realistic load
3. **Production Canary**: Enable for 10% traffic
4. **Full Rollout**: Enable for 100% after validation

---

## Rollback Plan

Set `eventara.evaluation.adaptive.enabled=false` - immediately reverts to per-event evaluation.

---

*Plan Version: 1.0*  
*Last Updated: 2026-02-04*
