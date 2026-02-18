# Mode A Deprecation Plan: Removing RealTimeRuleEvaluator

## Executive Summary

This document outlines the complete removal of **Mode A (RealTimeRuleEvaluator)** - the per-event rule evaluation system. This evaluator processes rules on **every single event** which creates `O(events × rules)` Redis queries, making it unsuitable for production workloads at scale.

We will transition to a **unified Adaptive Rate-Based Evaluation** system that evaluates rules at intelligent intervals based on traffic patterns.

---

## Why Remove Mode A?

### Performance Problems

| Metric | Mode A (Per-Event) | Mode B (Adaptive) |
|--------|:------------------:|:-----------------:|
| **Redis Queries** | O(events × rules) | O(rules / tick) |
| **At 1,000 EPS with 50 rules** | 50,000 queries/sec | ~50 queries/sec |
| **Latency added to event path** | 10-50ms blocking | 0ms (async) |
| **CPU usage** | High (inline processing) | Low (batched) |

### Code Complexity
- **Two evaluation paths** = double the maintenance burden
- **Configuration confusion** = users don't know which mode to use
- **Inconsistent behavior** = some rules work in one mode but not another

---

## Files to Remove/Modify

### Files to DELETE Completely

```
src/main/java/com/eventara/rule/evaluation/
├── RealTimeRuleEvaluator.java     ← DELETE (763 lines)
└── ThresholdState.java            ← DELETE (in-memory fallback, ~150 lines)
```

### Files to MODIFY

| File | Change |
|------|--------|
| `EventConsumer.java` | Remove RealTimeRuleEvaluator injection and conditional call |
| `AdaptiveEvaluationProperties.java` | Remove `enabled` flag (now always enabled) |
| `application.properties` | Remove `eventara.adaptive.enabled` config |
| `RuleServiceImpl.java` | Remove cache invalidation calls to RealTimeRuleEvaluator |
| Test files | Remove tests for Mode A, update tests for Mode B |

---

## Step-by-Step Removal Plan

### Phase 1: Extend AdaptiveRuleEvaluator (See: 02-adaptive-enhancement-plan.md)

**Before removing Mode A**, ensure AdaptiveRuleEvaluator supports ALL rule types:
- [x] Simple Threshold rules
- [ ] Composite rules (AND/OR)
- [ ] Event Ratio rules
- [ ] Rate of Change rules

> [!CAUTION]
> Do NOT proceed with Mode A removal until Phase 1 is complete and tested!

---

### Phase 2: Update EventConsumer

**Current code in `EventConsumer.java`:**
```java
// Rule Evaluation Strategy
if (adaptiveEvaluationProperties.isEnabled()) {
    // Adaptive evaluation
    adaptiveRuleEvaluator.onEventIngested(eventDto.isError());
} else {
    // Per-event evaluation
    realTimeRuleEvaluator.evaluateEvent(eventDto);
}
```

**New code (simplified):**
```java
// Adaptive rate-based rule evaluation (O(1) hot path)
adaptiveRuleEvaluator.onEventIngested(eventDto.isError());
```

---

### Phase 3: Remove RealTimeRuleEvaluator

1. **Delete file:** `src/main/java/com/eventara/rule/evaluation/RealTimeRuleEvaluator.java`
2. **Delete file:** `src/main/java/com/eventara/rule/evaluation/ThresholdState.java`
3. **Remove imports** from `EventConsumer.java`
4. **Remove field** `private RealTimeRuleEvaluator realTimeRuleEvaluator;`

---

### Phase 4: Simplify Configuration

**Remove from `AdaptiveEvaluationProperties.java`:**
```java
// REMOVE this field - adaptive is now the ONLY mode
private boolean enabled = true;
```

**Remove from `application.properties`:**
```properties
# REMOVE these lines
eventara.adaptive.enabled=true
```

**Update property class to just configure intervals:**
```java
@ConfigurationProperties(prefix = "eventara.evaluation")
public class EvaluationProperties {
    // Only interval configuration remains
    private long idleIntervalMs = 10_000;
    private long lowIntervalMs = 5_000;
    private long mediumIntervalMs = 2_000;
    private long highIntervalMs = 1_000;
    private long burstIntervalMs = 200;
    
    // EPS thresholds for tier switching
    private double lowEpsThreshold = 10;
    private double mediumEpsThreshold = 100;
    private double highEpsThreshold = 1000;
}
```

---

### Phase 5: Update RuleServiceImpl

**Remove cache invalidation calls to RealTimeRuleEvaluator:**

```java
// REMOVE this
@Autowired
private RealTimeRuleEvaluator realTimeRuleEvaluator;

// REMOVE these calls
realTimeRuleEvaluator.invalidateCache();
realTimeRuleEvaluator.refreshRuleCache();
```

**Keep only AdaptiveRuleEvaluator calls:**
```java
@Autowired
private AdaptiveRuleEvaluator adaptiveRuleEvaluator;

// On rule create/update/delete:
adaptiveRuleEvaluator.refreshRuleCache();
```

---

### Phase 6: Update Tests

**Files to update:**
- `tests/test_threshold_rules.py` - Ensure all tests pass with adaptive-only mode
- Any Java unit tests for `RealTimeRuleEvaluator` - Delete them

**Add new tests for:**
- Composite rule evaluation in adaptive mode
- Event Ratio rule evaluation in adaptive mode
- Rate of Change rule evaluation in adaptive mode

---

## Rollback Plan

If issues are discovered post-deployment:

1. **Git revert** the removal commits
2. **Restore** `RealTimeRuleEvaluator.java` from git history
3. **Set** `eventara.adaptive.enabled=false` in config
4. **Redeploy**

---

## Migration Checklist

- [ ] AdaptiveRuleEvaluator supports ALL rule types (Phase 1)
- [ ] All existing tests pass with adaptive-only mode
- [ ] Performance benchmarking shows improvement
- [ ] Documentation updated
- [ ] `EventConsumer.java` simplified
- [ ] `RealTimeRuleEvaluator.java` deleted
- [ ] `ThresholdState.java` deleted
- [ ] Configuration simplified
- [ ] `RuleServiceImpl.java` updated
- [ ] E2E tests pass
- [ ] Rollback plan tested

---

## Timeline Estimate

| Phase | Effort | Dependencies |
|-------|--------|--------------|
| Phase 1: Extend Adaptive | 4-6 hours | None |
| Phase 2: Update EventConsumer | 30 min | Phase 1 |
| Phase 3: Remove RealTimeRuleEvaluator | 30 min | Phase 2 |
| Phase 4: Simplify Configuration | 30 min | Phase 3 |
| Phase 5: Update RuleServiceImpl | 30 min | Phase 4 |
| Phase 6: Update Tests | 2-3 hours | Phase 5 |

**Total:** ~8-10 hours

---

## Success Criteria

1. **All rule types work** in adaptive mode (Composite, Ratio, RateOfChange)
2. **Zero performance regression** - actually should see improvement
3. **All tests pass** including new tests for complex rule types
4. **No Mode A code remains** in the codebase
5. **Configuration is simpler** - no `enabled` toggle needed
