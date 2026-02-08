# Phase 3: Evaluation Key for Rule Grouping

## Overview

**Goal**: Create a mechanism to group rules by their evaluation requirements, reducing Redis calls  
**Duration**: ~30 minutes  
**Difficulty**: Easy

---

## Why Rule Grouping?

**Problem**: Without grouping, 500 rules = 500 separate Redis calls per tick.

**Solution**: Group rules that need the same metrics (same window + filters), fetch once per group.

```
Before Grouping:
  Rule 1 (window=5min, global) → Redis call
  Rule 2 (window=5min, global) → Redis call  ← DUPLICATE!
  Rule 3 (window=5min, global) → Redis call  ← DUPLICATE!
  Rule 4 (window=10min, source=api) → Redis call
  Rule 5 (window=10min, source=api) → Redis call  ← DUPLICATE!

After Grouping:
  Group A (window=5min, global) → 1 Redis call → Evaluate Rules 1, 2, 3
  Group B (window=10min, source=api) → 1 Redis call → Evaluate Rules 4, 5
  
Result: 5 Redis calls → 2 Redis calls (60% reduction!)
```

---

## Prerequisites

- [x] Phase 1 completed (Configuration)
- [x] Phase 2 completed (EventRateMonitor)
- [x] Project compiles successfully

---

## Step 1: Create EvaluationKey.java

**File Path:** `src/main/java/com/eventara/rule/evaluation/EvaluationKey.java`

Create this file and paste the following code:

```java
package com.eventara.rule.evaluation;

import com.eventara.rule.entity.AlertRule;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Key that determines which rules can share the same metrics fetch.
 * 
 * Rules with the same EvaluationKey need the same Redis data,
 * so they can share a single Redis call.
 * 
 * Grouping is based on:
 * - windowMinutes: The time window for metrics aggregation
 * - sourceFilter: List of sources to filter by (null = global)
 * - eventTypeFilter: List of event types to filter by (null = all)
 */
public class EvaluationKey {

    private final int windowMinutes;
    private final List<String> sourceFilter;
    private final List<String> eventTypeFilter;

    /**
     * Create an EvaluationKey with explicit values.
     */
    public EvaluationKey(int windowMinutes, List<String> sourceFilter, List<String> eventTypeFilter) {
        this.windowMinutes = windowMinutes;
        this.sourceFilter = sourceFilter;
        this.eventTypeFilter = eventTypeFilter;
    }

    /**
     * Extract an EvaluationKey from a rule's configuration.
     * 
     * @param rule The AlertRule to extract key from
     * @param defaultWindow Default window minutes if not specified in rule
     * @return EvaluationKey for this rule
     */
    @SuppressWarnings("unchecked")
    public static EvaluationKey fromRule(AlertRule rule, int defaultWindow) {
        Map<String, Object> config = rule.getRuleConfig();
        
        if (config == null) {
            return new EvaluationKey(defaultWindow, null, null);
        }

        // Extract window minutes
        int window = defaultWindow;
        if (config.containsKey("timeWindowMinutes")) {
            Object val = config.get("timeWindowMinutes");
            if (val instanceof Number) {
                window = ((Number) val).intValue();
            } else if (val instanceof String) {
                try {
                    window = Integer.parseInt((String) val);
                } catch (NumberFormatException e) {
                    // Use default
                }
            }
        }

        // Extract source filter
        List<String> sources = null;
        if (config.containsKey("sourceFilter")) {
            Object val = config.get("sourceFilter");
            if (val instanceof List) {
                sources = (List<String>) val;
                if (sources.isEmpty()) {
                    sources = null; // Empty list = no filter = global
                }
            }
        }

        // Extract event type filter
        List<String> types = null;
        if (config.containsKey("eventTypeFilter")) {
            Object val = config.get("eventTypeFilter");
            if (val instanceof List) {
                types = (List<String>) val;
                if (types.isEmpty()) {
                    types = null; // Empty list = no filter = all types
                }
            }
        }

        return new EvaluationKey(window, sources, types);
    }

    // ========== Getters ==========

    public int getWindowMinutes() {
        return windowMinutes;
    }

    public List<String> getSourceFilter() {
        return sourceFilter;
    }

    public List<String> getEventTypeFilter() {
        return eventTypeFilter;
    }

    // ========== Grouping Support ==========

    /**
     * Check if this key represents a global (unfiltered) evaluation.
     */
    public boolean isGlobal() {
        return (sourceFilter == null || sourceFilter.isEmpty()) &&
               (eventTypeFilter == null || eventTypeFilter.isEmpty());
    }

    /**
     * Check if this key has a source filter.
     */
    public boolean hasSourceFilter() {
        return sourceFilter != null && !sourceFilter.isEmpty();
    }

    /**
     * Check if this key has an event type filter.
     */
    public boolean hasEventTypeFilter() {
        return eventTypeFilter != null && !eventTypeFilter.isEmpty();
    }

    /**
     * Get a human-readable description of this key.
     * Useful for logging and debugging.
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("window=").append(windowMinutes).append("min");
        
        if (hasSourceFilter()) {
            sb.append(", sources=").append(sourceFilter);
        }
        if (hasEventTypeFilter()) {
            sb.append(", types=").append(eventTypeFilter);
        }
        if (isGlobal()) {
            sb.append(" (global)");
        }
        
        return sb.toString();
    }

    // ========== Equality (Required for HashMap/groupingBy) ==========

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

    @Override
    public String toString() {
        return "EvaluationKey{" + getDescription() + "}";
    }
}
```

---

## Step 2: Verify the Build

Run Maven to verify everything compiles:

```bash
mvn compile
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
```

---

## Step 3: Understanding Rule Grouping

### How Grouping Works

```java
// In AdaptiveRuleEvaluator (Phase 4), we'll do:

List<AlertRule> rules = getActiveThresholdRules();  // e.g., 500 rules

// Group by evaluation key
Map<EvaluationKey, List<AlertRule>> groups = rules.stream()
    .collect(Collectors.groupingBy(r -> EvaluationKey.fromRule(r, defaultWindow)));

// Typically results in 10-50 groups instead of 500 individual calls!

for (Map.Entry<EvaluationKey, List<AlertRule>> entry : groups.entrySet()) {
    EvaluationKey key = entry.getKey();
    List<AlertRule> groupRules = entry.getValue();
    
    // ONE Redis call for this group
    MetricsBucket bucket = fetchMetricsForKey(key);
    
    // Evaluate all rules in group against same bucket
    for (AlertRule rule : groupRules) {
        evaluateRuleAgainstBucket(rule, bucket);
    }
}
```

### Example Grouping

| Rule Name | Window | Source Filter | Type Filter | EvaluationKey |
|-----------|--------|---------------|-------------|---------------|
| High Error Rate | 5 min | null | null | `{5, null, null}` |
| Error Spike | 5 min | null | null | `{5, null, null}` ← Same group! |
| API Latency | 5 min | [api-server] | null | `{5, [api-server], null}` |
| API Errors | 5 min | [api-server] | null | `{5, [api-server], null}` ← Same! |
| Long Window | 15 min | null | null | `{15, null, null}` |

**Result**: 5 rules → 3 groups → 3 Redis calls

---

## Directory Structure After Phase 3

```
src/main/java/com/eventara/rule/evaluation/
├── config/
│   └── AdaptiveEvaluationProperties.java   ← Phase 1
├── EvaluationKey.java                      ← NEW (Phase 3)
├── EventRateMonitor.java                   ← Phase 2
├── RealTimeRuleEvaluator.java              ← Existing
└── ThresholdState.java                     ← Existing
```

---

## API Reference

| Method | Description |
|--------|-------------|
| `fromRule(AlertRule, int)` | Extract key from rule config |
| `getWindowMinutes()` | Get time window in minutes |
| `getSourceFilter()` | Get source filter list (null = global) |
| `getEventTypeFilter()` | Get type filter list (null = all) |
| `isGlobal()` | Check if no filters applied |
| `getDescription()` | Human-readable description |

---

## ✅ Phase 3 Complete!

You have successfully:
- [x] Created `EvaluationKey.java`
- [x] Verified the build compiles

---

## Next Step

Proceed to **Phase 4: Adaptive Rule Evaluator** - the main evaluation service that ties everything together.

---

*Phase 3 Complete | Total Est. Time: 30 minutes*
