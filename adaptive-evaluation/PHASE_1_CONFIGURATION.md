# Phase 1: Configuration Infrastructure

## Overview

**Goal**: Add configuration classes for Adaptive Rate-Based Evaluation  
**Duration**: ~30 minutes  
**Difficulty**: Easy

---

## Prerequisites

Make sure you have:
- [ ] Java 17+ installed
- [ ] Project builds successfully (`mvn compile`)
- [ ] IDE open with the Eventara project

---

## Step 1: Create the Config Directory

Create a new directory for the evaluation configuration:

```
src/main/java/com/eventara/rule/evaluation/config/
```

**Terminal command:**
```bash
mkdir -p src/main/java/com/eventara/rule/evaluation/config
```

---

## Step 2: Create AdaptiveEvaluationProperties.java

**File Path:** `src/main/java/com/eventara/rule/evaluation/config/AdaptiveEvaluationProperties.java`

Create this file and paste the following code:

```java
package com.eventara.rule.evaluation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Adaptive Rate-Based Evaluation.
 * 
 * This enables dynamic adjustment of rule evaluation frequency
 * based on the incoming event rate, optimizing for both low-latency
 * alerting during high traffic and resource conservation during idle periods.
 * 
 * Loaded from application.properties with prefix "eventara.evaluation.adaptive"
 */
@Configuration
@ConfigurationProperties(prefix = "eventara.evaluation.adaptive")
public class AdaptiveEvaluationProperties {

    /**
     * Master switch to enable/disable adaptive evaluation.
     * When false, falls back to per-event evaluation.
     */
    private boolean enabled = false;
    
    /**
     * Event rate thresholds (events per second) that determine
     * which evaluation interval to use.
     */
    private Thresholds thresholds = new Thresholds();
    
    /**
     * Evaluation intervals (milliseconds) for each traffic tier.
     */
    private Intervals intervals = new Intervals();

    /**
     * Event rate thresholds configuration.
     * Values are in events per second (EPS).
     */
    public static class Thresholds {
        
        /**
         * Below this EPS = idle mode (e.g., < 0.1 = less than 5 events/min)
         */
        private double idle = 0.1;
        
        /**
         * Below this EPS = low traffic mode (e.g., < 1.0 = less than 50 events/min)
         */
        private double low = 1.0;
        
        /**
         * Below this EPS = medium traffic mode (e.g., < 10.0 = less than 500 events/min)
         */
        private double medium = 10.0;
        
        /**
         * Below this EPS = high traffic mode (e.g., < 100.0 = less than 5000 events/min)
         * Above this = burst mode
         */
        private double high = 100.0;

        // ========== Getters and Setters ==========
        
        public double getIdle() {
            return idle;
        }

        public void setIdle(double idle) {
            this.idle = idle;
        }

        public double getLow() {
            return low;
        }

        public void setLow(double low) {
            this.low = low;
        }

        public double getMedium() {
            return medium;
        }

        public void setMedium(double medium) {
            this.medium = medium;
        }

        public double getHigh() {
            return high;
        }

        public void setHigh(double high) {
            this.high = high;
        }
    }

    /**
     * Evaluation interval configuration.
     * Values are in milliseconds.
     */
    public static class Intervals {
        
        /**
         * Evaluation interval when in idle mode (very low traffic).
         * Default: 30 seconds
         */
        private long idleMs = 30000;
        
        /**
         * Evaluation interval when in low traffic mode.
         * Default: 10 seconds
         */
        private long lowMs = 10000;
        
        /**
         * Evaluation interval when in medium traffic mode.
         * Default: 2 seconds
         */
        private long mediumMs = 2000;
        
        /**
         * Evaluation interval when in high traffic mode.
         * Default: 500 milliseconds
         */
        private long highMs = 500;
        
        /**
         * Evaluation interval when in burst mode (very high traffic).
         * Default: 100 milliseconds
         */
        private long burstMs = 100;

        // ========== Getters and Setters ==========
        
        public long getIdleMs() {
            return idleMs;
        }

        public void setIdleMs(long idleMs) {
            this.idleMs = idleMs;
        }

        public long getLowMs() {
            return lowMs;
        }

        public void setLowMs(long lowMs) {
            this.lowMs = lowMs;
        }

        public long getMediumMs() {
            return mediumMs;
        }

        public void setMediumMs(long mediumMs) {
            this.mediumMs = mediumMs;
        }

        public long getHighMs() {
            return highMs;
        }

        public void setHighMs(long highMs) {
            this.highMs = highMs;
        }

        public long getBurstMs() {
            return burstMs;
        }

        public void setBurstMs(long burstMs) {
            this.burstMs = burstMs;
        }
    }

    // ========== Root Getters and Setters ==========

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Thresholds getThresholds() {
        return thresholds;
    }

    public void setThresholds(Thresholds thresholds) {
        this.thresholds = thresholds;
    }

    public Intervals getIntervals() {
        return intervals;
    }

    public void setIntervals(Intervals intervals) {
        this.intervals = intervals;
    }
    
    // ========== Convenience Methods ==========
    
    /**
     * Get the appropriate interval for a given events-per-second rate.
     * 
     * @param eps Current events per second
     * @return Interval in milliseconds
     */
    public long getIntervalForRate(double eps) {
        if (eps < thresholds.idle) return intervals.idleMs;
        if (eps < thresholds.low) return intervals.lowMs;
        if (eps < thresholds.medium) return intervals.mediumMs;
        if (eps < thresholds.high) return intervals.highMs;
        return intervals.burstMs;
    }
    
    /**
     * Get the traffic tier name for a given events-per-second rate.
     * Useful for logging and monitoring.
     * 
     * @param eps Current events per second
     * @return Tier name (IDLE, LOW, MEDIUM, HIGH, BURST)
     */
    public String getTierForRate(double eps) {
        if (eps < thresholds.idle) return "IDLE";
        if (eps < thresholds.low) return "LOW";
        if (eps < thresholds.medium) return "MEDIUM";
        if (eps < thresholds.high) return "HIGH";
        return "BURST";
    }
}
```

---

## Step 3: Update application.properties

**File Path:** `src/main/resources/application.properties`

Add the following lines at the end of the file:

```properties
# =========================
# Adaptive Evaluation Configuration
# =========================
# Master switch - set to true to enable adaptive evaluation
eventara.evaluation.adaptive.enabled=${EVENTARA_ADAPTIVE_ENABLED:false}

# Event rate thresholds (events per second)
# Determines which evaluation interval tier is used
eventara.evaluation.adaptive.thresholds.idle=0.1
eventara.evaluation.adaptive.thresholds.low=1.0
eventara.evaluation.adaptive.thresholds.medium=10.0
eventara.evaluation.adaptive.thresholds.high=100.0

# Evaluation intervals (milliseconds)
# How often to evaluate rules in each traffic tier
eventara.evaluation.adaptive.intervals.idle-ms=30000
eventara.evaluation.adaptive.intervals.low-ms=10000
eventara.evaluation.adaptive.intervals.medium-ms=2000
eventara.evaluation.adaptive.intervals.high-ms=500
eventara.evaluation.adaptive.intervals.burst-ms=100
```

---

## Step 4: Verify the Build

Run Maven to verify everything compiles:

```bash
mvn compile
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
```

If you see any errors, check:
1. Package name matches the directory structure
2. All imports are correct
3. No typos in the code

---

## Step 5: Verify Configuration Loading (Optional)

Create a quick test to verify the configuration loads correctly.

You can add this temporary code to any `@PostConstruct` method or create a test:

```java
@Autowired
private AdaptiveEvaluationProperties adaptiveConfig;

@PostConstruct
public void testConfig() {
    System.out.println("=== Adaptive Evaluation Config ===");
    System.out.println("Enabled: " + adaptiveConfig.isEnabled());
    System.out.println("Idle threshold: " + adaptiveConfig.getThresholds().getIdle() + " EPS");
    System.out.println("Idle interval: " + adaptiveConfig.getIntervals().getIdleMs() + " ms");
    System.out.println("================================");
}
```

---

## Directory Structure After Phase 1

```
src/main/java/com/eventara/rule/evaluation/
├── config/
│   └── AdaptiveEvaluationProperties.java   ← NEW
├── RealTimeRuleEvaluator.java              ← Existing
└── ThresholdState.java                     ← Existing

src/main/resources/
└── application.properties                   ← MODIFIED (added new properties)
```

---

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `eventara.evaluation.adaptive.enabled` | `false` | Master switch |
| `eventara.evaluation.adaptive.thresholds.idle` | `0.1` | EPS threshold for idle mode |
| `eventara.evaluation.adaptive.thresholds.low` | `1.0` | EPS threshold for low traffic |
| `eventara.evaluation.adaptive.thresholds.medium` | `10.0` | EPS threshold for medium traffic |
| `eventara.evaluation.adaptive.thresholds.high` | `100.0` | EPS threshold for high traffic |
| `eventara.evaluation.adaptive.intervals.idle-ms` | `30000` | Interval when idle (30s) |
| `eventara.evaluation.adaptive.intervals.low-ms` | `10000` | Interval for low traffic (10s) |
| `eventara.evaluation.adaptive.intervals.medium-ms` | `2000` | Interval for medium traffic (2s) |
| `eventara.evaluation.adaptive.intervals.high-ms` | `500` | Interval for high traffic (500ms) |
| `eventara.evaluation.adaptive.intervals.burst-ms` | `100` | Interval for burst mode (100ms) |

---

## Interval Behavior Chart

```
Events/sec    Traffic Tier    Eval Interval    Alert Latency (worst)
──────────────────────────────────────────────────────────────────
< 0.1         IDLE           30 seconds        ~30 seconds
0.1 - 1.0     LOW            10 seconds        ~10 seconds
1.0 - 10.0    MEDIUM          2 seconds         ~2 seconds
10.0 - 100.0  HIGH          500 milliseconds   ~500 ms
> 100.0       BURST         100 milliseconds   ~100 ms
```

---

## ✅ Phase 1 Complete!

You have successfully:
- [x] Created the configuration directory
- [x] Created `AdaptiveEvaluationProperties.java`
- [x] Added configuration to `application.properties`
- [x] Verified the build compiles

---

## Next Step

Proceed to **Phase 2: Event Rate Monitor** to create the component that tracks incoming event rates.

---

*Phase 1 Complete | Total Est. Time: 30 minutes*
