# Adaptive Rule Evaluator Enhancement Plan

## Executive Summary

This document outlines the enhancement of **AdaptiveRuleEvaluator** to become the **single, production-grade rule evaluation engine** for Eventara. We will add support for:

- **Composite Rules** (AND/OR conditions)
- **Event Ratio Rules** (conversion rates)
- **Rate of Change Rules** (spike detection)

The goal is a **unified, scalable, and maintainable** rule evaluation system.

---

## Current Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     EventConsumer                               │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │    onEventIngested(isError)                                │ │
│  │         ↓                                                  │ │
│  │    dirtyFlag.set(true)  ← O(1) HOT PATH                   │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│               AdaptiveRuleEvaluator (Background)                │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  @Scheduled(fixedRate = 100ms)                             │ │
│  │  checkAndEvaluate()                                        │ │
│  │      ↓                                                     │ │
│  │  evaluateAllRulesGrouped()                                 │ │
│  │      ↓                                                     │ │
│  │  [Group by EvaluationKey] → [Fetch Once] → [Eval Many]     │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

**Current Limitation:** Only supports simple threshold rules. Composite, Ratio, and RateOfChange rules are skipped.

---

## Target Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│               AdaptiveRuleEvaluator (Enhanced)                  │
│                                                                 │
│  evaluateAllRulesGrouped()                                      │
│      ↓                                                          │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  Rule Type Router                                          │ │
│  │    ├─ SIMPLE    → SimpleThresholdHandler                  │ │
│  │    ├─ COMPOSITE → CompositeRuleHandler                    │ │
│  │    ├─ RATIO     → EventRatioHandler                       │ │
│  │    └─ CHANGE    → RateOfChangeHandler                     │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                 │
│  [All handlers share the same MetricsBucket from Redis]         │
└─────────────────────────────────────────────────────────────────┘
```

---

## Design Principles

### 1. Handler Pattern

Instead of giant `switch` statements, use the **Strategy Pattern** with specialized handlers:

```java
public interface RuleHandler {
    /**
     * Check if this handler can process the given rule.
     */
    boolean canHandle(AlertRule rule);
    
    /**
     * Evaluate the rule against the given metrics.
     * Returns Optional.of(EvaluationResult) if threshold crossed.
     */
    Optional<EvaluationResult> evaluate(AlertRule rule, MetricsBucket bucket, int windowMinutes);
}
```

### 2. Shared Metrics Fetching

All handlers receive pre-fetched `MetricsBucket` - NO handler should fetch Redis directly. This maintains the O(rules/tick) optimization.

### 3. Cooldown Remains Centralized

The cooldown logic stays in `AdaptiveRuleEvaluator`, not in individual handlers.

---

## Implementation Details

### New Files to Create

```
src/main/java/com/eventara/rule/evaluation/handler/
├── RuleHandler.java                    # Interface
├── HandlerRegistry.java                # Handler discovery/routing
├── SimpleThresholdHandler.java         # Existing logic, refactored
├── CompositeRuleHandler.java           # NEW: AND/OR conditions  
├── EventRatioHandler.java              # NEW: Conversion rates
└── RateOfChangeHandler.java            # NEW: Spike detection
```

### Model Classes

```
src/main/java/com/eventara/rule/evaluation/model/
├── EvaluationResult.java               # Result of rule evaluation
└── RuleContext.java                    # Context passed to handlers
```

---

## Handler Implementations

### 1. SimpleThresholdHandler (Refactor from existing)

```java
@Component
public class SimpleThresholdHandler implements RuleHandler {
    
    @Override
    public boolean canHandle(AlertRule rule) {
        Map<String, Object> config = rule.getRuleConfig();
        // Simple threshold: has metricType, condition, thresholdValue
        // NO conditions array, NO special metric types
        return config.containsKey("metricType") 
            && !config.containsKey("conditions")
            && !isSpecialMetric((String) config.get("metricType"));
    }
    
    @Override
    public Optional<EvaluationResult> evaluate(AlertRule rule, MetricsBucket bucket, int windowMinutes) {
        Map<String, Object> config = rule.getRuleConfig();
        String metricType = (String) config.get("metricType");
        String condition = (String) config.get("condition");
        double threshold = parseDouble(config.get("thresholdValue"));
        
        double currentValue = getMetricValue(metricType, bucket, windowMinutes);
        boolean crossed = isThresholdCrossed(condition, currentValue, threshold);
        
        if (crossed) {
            return Optional.of(new EvaluationResult(currentValue, threshold, metricType));
        }
        return Optional.empty();
    }
}
```

### 2. CompositeRuleHandler (NEW)

Handles rules with `conditions` array and `operator` (AND/OR):

```java
@Component
@RequiredArgsConstructor
public class CompositeRuleHandler implements RuleHandler {
    
    private final RedisMetricsService redisMetrics; // For fetching additional buckets if needed
    
    @Override
    public boolean canHandle(AlertRule rule) {
        return rule.getRuleConfig().containsKey("conditions");
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Optional<EvaluationResult> evaluate(AlertRule rule, MetricsBucket bucket, int windowMinutes) {
        Map<String, Object> config = rule.getRuleConfig();
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) config.get("conditions");
        String operator = (String) config.getOrDefault("operator", "AND");
        
        boolean isAnd = "AND".equalsIgnoreCase(operator);
        boolean result = isAnd; // AND starts true, OR starts false
        
        StringBuilder details = new StringBuilder();
        double primaryValue = 0;
        double primaryThreshold = 0;
        boolean first = true;
        
        for (Map<String, Object> cond : conditions) {
            String metricType = (String) cond.get("metricType");
            String condOp = (String) cond.get("condition");
            double threshold = parseDouble(cond.get("value"));
            
            double currentValue = getMetricValue(metricType, bucket, windowMinutes);
            boolean conditionMet = isThresholdCrossed(condOp, currentValue, threshold);
            
            if (first) {
                primaryValue = currentValue;
                primaryThreshold = threshold;
                first = false;
            }
            
            // Build details for logging
            details.append(String.format("%s(%.2f) %s %.2f: %s | ",
                metricType, currentValue, condOp, threshold, conditionMet ? "✓" : "✗"));
            
            // Apply operator
            if (isAnd) {
                result = result && conditionMet;
            } else {
                result = result || conditionMet;
            }
        }
        
        if (result) {
            return Optional.of(new EvaluationResult(primaryValue, primaryThreshold, 
                "COMPOSITE[" + operator + "]: " + details));
        }
        return Optional.empty();
    }
}
```

### 3. EventRatioHandler (NEW)

Handles `metricType: EVENT_RATIO` with numerator/denominator event types:

```java
@Component
@RequiredArgsConstructor
public class EventRatioHandler implements RuleHandler {
    
    private final RedisMetricsService redisMetrics;
    
    @Override
    public boolean canHandle(AlertRule rule) {
        String metricType = (String) rule.getRuleConfig().get("metricType");
        return "EVENT_RATIO".equals(metricType);
    }
    
    @Override
    public Optional<EvaluationResult> evaluate(AlertRule rule, MetricsBucket bucket, int windowMinutes) {
        Map<String, Object> config = rule.getRuleConfig();
        
        String numeratorType = (String) config.get("numeratorEventType");
        String denominatorType = (String) config.get("denominatorEventType");
        String condition = (String) config.get("condition");
        double threshold = parseDouble(config.get("thresholdValue"));
        int minDenominator = parseIntOrDefault(config.get("minDenominatorEvents"), 5);
        
        // Fetch metrics for both event types
        // NOTE: This is an exception to "no Redis in handlers" rule
        // because ratio rules REQUIRE two different event type buckets
        MetricsBucket numeratorBucket = redisMetrics.getMetricsForEventType(numeratorType, windowMinutes);
        MetricsBucket denominatorBucket = redisMetrics.getMetricsForEventType(denominatorType, windowMinutes);
        
        long numeratorCount = numeratorBucket.getTotalEvents();
        long denominatorCount = denominatorBucket.getTotalEvents();
        
        // Check minimum denominator
        if (denominatorCount < minDenominator) {
            return Optional.empty();
        }
        
        double ratio = (double) numeratorCount / denominatorCount;
        boolean crossed = isThresholdCrossed(condition, ratio, threshold);
        
        if (crossed) {
            String details = String.format("%s/%s = %d/%d = %.3f", 
                numeratorType, denominatorType, numeratorCount, denominatorCount, ratio);
            return Optional.of(new EvaluationResult(ratio, threshold, details));
        }
        return Optional.empty();
    }
}
```

### 4. RateOfChangeHandler (NEW)

Handles metrics ending in `_CHANGE` (e.g., `ERROR_RATE_CHANGE`, `LATENCY_CHANGE`):

```java
@Component
@RequiredArgsConstructor
public class RateOfChangeHandler implements RuleHandler {
    
    private final RedisMetricsService redisMetrics;
    
    private static final Set<String> CHANGE_METRICS = Set.of(
        "ERROR_RATE_CHANGE", "LATENCY_CHANGE", "THROUGHPUT_CHANGE", "SPIKE_DETECTION"
    );
    
    @Override
    public boolean canHandle(AlertRule rule) {
        String metricType = (String) rule.getRuleConfig().get("metricType");
        return metricType != null && CHANGE_METRICS.contains(metricType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Optional<EvaluationResult> evaluate(AlertRule rule, MetricsBucket currentBucket, int windowMinutes) {
        Map<String, Object> config = rule.getRuleConfig();
        String metricType = (String) config.get("metricType");
        String condition = (String) config.get("condition");
        double threshold = parseDouble(config.get("thresholdValue"));
        
        // Get PREVIOUS window bucket for comparison
        MetricsBucket previousBucket;
        if (config.containsKey("sourceFilter")) {
            List<String> sources = (List<String>) config.get("sourceFilter");
            if (sources != null && !sources.isEmpty()) {
                previousBucket = redisMetrics.getMetricsForSourcePreviousWindow(sources.get(0), windowMinutes);
            } else {
                previousBucket = redisMetrics.getMetricsPreviousWindow(windowMinutes);
            }
        } else {
            previousBucket = redisMetrics.getMetricsPreviousWindow(windowMinutes);
        }
        
        // Get the base metric type (ERROR_RATE_CHANGE → ERROR_RATE)
        String baseMetric = getBaseMetric(metricType);
        double currentValue = getMetricValue(baseMetric, currentBucket, windowMinutes);
        double previousValue = getMetricValue(baseMetric, previousBucket, windowMinutes);
        
        // Calculate % change
        double percentChange;
        if (previousValue == 0) {
            percentChange = currentValue > 0 ? 100.0 : 0;
        } else {
            percentChange = ((currentValue - previousValue) / previousValue) * 100.0;
        }
        
        boolean crossed = isThresholdCrossed(condition, percentChange, threshold);
        
        if (crossed) {
            String details = String.format("%s: %.1f%% change (%.2f → %.2f)", 
                baseMetric, percentChange, previousValue, currentValue);
            return Optional.of(new EvaluationResult(percentChange, threshold, details));
        }
        return Optional.empty();
    }
    
    private String getBaseMetric(String changeMetric) {
        switch (changeMetric) {
            case "ERROR_RATE_CHANGE": return "ERROR_RATE";
            case "LATENCY_CHANGE": return "AVG_LATENCY";
            case "THROUGHPUT_CHANGE": return "EVENTS_PER_MINUTE";
            case "SPIKE_DETECTION": return "TOTAL_EVENTS";
            default: return "ERROR_RATE";
        }
    }
}
```

---

## HandlerRegistry

Central registry that routes rules to appropriate handlers:

```java
@Component
@RequiredArgsConstructor
public class HandlerRegistry {
    
    private final List<RuleHandler> handlers;
    
    /**
     * Find the appropriate handler for a rule.
     * Returns Optional.empty() if no handler can process it.
     */
    public Optional<RuleHandler> findHandler(AlertRule rule) {
        return handlers.stream()
            .filter(h -> h.canHandle(rule))
            .findFirst();
    }
}
```

---

## Updated AdaptiveRuleEvaluator

The evaluator becomes simpler - it just routes to handlers:

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class AdaptiveRuleEvaluator {
    
    private final HandlerRegistry handlerRegistry;
    private final AlertTriggerHandler alertHandler;
    // ... other dependencies
    
    private void evaluateRuleAgainstBucket(AlertRule rule, MetricsBucket bucket, int windowMinutes) {
        Optional<RuleHandler> handler = handlerRegistry.findHandler(rule);
        
        if (handler.isEmpty()) {
            log.warn("No handler found for rule: {} (type={})", 
                rule.getName(), rule.getRuleConfig().get("metricType"));
            return;
        }
        
        int cooldownMinutes = parseIntOrDefault(
            rule.getRuleConfig().get("cooldownMinutes"), 5);
        
        try {
            Optional<EvaluationResult> result = handler.get()
                .evaluate(rule, bucket, windowMinutes);
            
            if (result.isPresent() && !isInCooldown(rule.getId(), cooldownMinutes)) {
                EvaluationResult r = result.get();
                log.info("🚨 Rule '{}' triggered: {}", rule.getName(), r.getDetails());
                fireAlert(rule, r.getCurrentValue(), r.getThreshold());
                setCooldown(rule.getId(), cooldownMinutes);
            }
        } catch (Exception e) {
            log.error("Handler error for rule {}: {}", rule.getId(), e.getMessage());
        }
    }
}
```

---

## EvaluationResult Model

```java
@Data
@AllArgsConstructor
public class EvaluationResult {
    private final double currentValue;
    private final double threshold;
    private final String details;  // Human-readable description
}
```

---

## Performance Considerations

### Ratio and RateOfChange Handlers Need Extra Redis Calls

Unlike SimpleThreshold, these handlers need additional data:
- **Ratio:** Two event-type buckets (numerator, denominator)
- **RateOfChange:** Previous window bucket

**Optimization Options:**

1. **Batched Pre-fetch:** Before handler evaluation, identify all unique fetch requirements and batch them
2. **Cache:** Short-TTL cache for buckets (100-500ms) to avoid duplicate fetches
3. **Accept the cost:** For most deployments, a few extra Redis calls per tick are negligible

**Recommended:** Option 3 for simplicity. The cost is minimal because:
- Ratio/Change rules are typically less common than simple thresholds
- Redis calls are ~1ms each
- We're already saving 100x+ calls by not evaluating per-event

---

## Testing Strategy

### Unit Tests

Each handler should have comprehensive unit tests:

```java
@Test
void compositeAndRule_allConditionsMet_shouldTrigger() {
    AlertRule rule = createCompositeRule("AND", List.of(
        condition("ERROR_RATE", "GREATER_THAN", 5.0),
        condition("AVG_LATENCY", "GREATER_THAN", 500)
    ));
    
    MetricsBucket bucket = createBucket(errorRate: 10.0, avgLatency: 1000);
    
    Optional<EvaluationResult> result = handler.evaluate(rule, bucket, 5);
    
    assertThat(result).isPresent();
}

@Test
void compositeAndRule_oneConditionFails_shouldNotTrigger() {
    // ...
}
```

### Integration Tests

Test the full flow from event ingestion to alert:

1. Create rule via API
2. Ingest events
3. Wait for evaluation tick
4. Verify Redis cooldown key exists
5. Verify alert history created

### Performance Tests

Benchmark with:
- 50 rules (mix of types)
- 1000 EPS event rate
- Measure Redis query count per minute
- Compare against per-event evaluation

---

## Implementation Order

| Step | Component | Effort |
|------|-----------|--------|
| 1 | Create `RuleHandler` interface | 15 min |
| 2 | Create `EvaluationResult` model | 10 min |
| 3 | Create `SimpleThresholdHandler` (refactor existing) | 30 min |
| 4 | Create `CompositeRuleHandler` | 45 min |
| 5 | Create `EventRatioHandler` | 45 min |
| 6 | Create `RateOfChangeHandler` | 45 min |
| 7 | Create `HandlerRegistry` | 20 min |
| 8 | Update `AdaptiveRuleEvaluator` to use handlers | 30 min |
| 9 | Remove VIRTUAL_METRICS skip logic | 10 min |
| 10 | Write unit tests for each handler | 2 hours |
| 11 | Write integration tests | 1 hour |

**Total: ~6-7 hours**

---

## Success Criteria

1. ✅ All simple threshold rules work (existing functionality preserved)
2. ✅ Composite AND rules trigger only when ALL conditions met
3. ✅ Composite OR rules trigger when ANY condition met
4. ✅ Event Ratio rules calculate correctly with min denominator check
5. ✅ Rate of Change rules compare current vs previous window correctly
6. ✅ All rule types respect cooldown
7. ✅ Performance: No per-event Redis calls
8. ✅ Python test suite passes with all rule types
9. ✅ Handler pattern is extensible for future rule types

---

## Future Extensibility

With the handler pattern, adding new rule types is easy:

1. Create new `XxxHandler implements RuleHandler`
2. Implement `canHandle()` and `evaluate()`
3. Add `@Component` annotation
4. Done! Spring auto-discovers and registers it

**Potential future handlers:**
- `WindowedAnomalyHandler` - Standard deviation based detection
- `SeasonalBaselineHandler` - Compare to historical patterns
- `MachineLearningHandler` - ML model-based anomaly detection
