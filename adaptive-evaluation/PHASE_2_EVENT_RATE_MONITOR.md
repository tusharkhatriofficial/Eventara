# Phase 2: Event Rate Monitor

## Overview

**Goal**: Create a lightweight component to track incoming event rate (EPS)  
**Duration**: ~45 minutes  
**Difficulty**: Easy

---

## Prerequisites

- [x] Phase 1 completed (AdaptiveEvaluationProperties created)
- [x] Project compiles successfully

---

## Step 1: Create EventRateMonitor.java

**File Path:** `src/main/java/com/eventara/rule/evaluation/EventRateMonitor.java`

Create this file and paste the following code:

```java
package com.eventara.rule.evaluation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight event rate monitor for adaptive evaluation.
 * 
 * This component tracks the incoming event rate using atomic counters
 * and calculates a smoothed Events Per Second (EPS) value using
 * Exponential Moving Average (EMA) to prevent rapid oscillation.
 * 
 * All operations are O(1) and thread-safe.
 */
@Component
@Slf4j
public class EventRateMonitor {

    /**
     * Counter for total events since last sample.
     * Reset to 0 after each sample.
     */
    private final AtomicLong eventCounter = new AtomicLong(0);
    
    /**
     * Counter for error events since last sample.
     * Reset to 0 after each sample.
     */
    private final AtomicLong errorCounter = new AtomicLong(0);
    
    /**
     * Timestamp of the last EPS sample.
     */
    private volatile long lastSampleTime = System.currentTimeMillis();
    
    /**
     * Current smoothed events per second.
     */
    private volatile double currentEps = 0.0;
    
    /**
     * Current smoothed errors per second.
     */
    private volatile double currentErrorEps = 0.0;
    
    /**
     * Exponential Moving Average factor.
     * 0.3 means 30% weight to new value, 70% to historical.
     * Lower values = smoother but slower to react.
     * Higher values = faster reaction but more jitter.
     */
    private static final double EMA_ALPHA = 0.3;
    
    /**
     * Minimum interval between samples (milliseconds).
     * Prevents division by very small numbers.
     */
    private static final long MIN_SAMPLE_INTERVAL_MS = 100;

    /**
     * Record an event arrival.
     * Called from EventConsumer on every Kafka message.
     * 
     * This is O(1) and thread-safe - just increments atomic counters.
     * 
     * @param isError true if this event is an error event
     */
    public void recordEvent(boolean isError) {
        eventCounter.incrementAndGet();
        if (isError) {
            errorCounter.incrementAndGet();
        }
    }
    
    /**
     * Record an event arrival (simple version).
     * Called when error status is not known or not relevant.
     */
    public void recordEvent() {
        eventCounter.incrementAndGet();
    }

    /**
     * Sample the current event rate and calculate smoothed EPS.
     * 
     * This method:
     * 1. Calculates instant EPS from counter and elapsed time
     * 2. Applies Exponential Moving Average for smoothing
     * 3. Resets counters for next sampling period
     * 
     * Should be called periodically (e.g., every tick or every second).
     * 
     * @return Smoothed events per second
     */
    public double sampleAndGetEps() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastSampleTime;
        
        // Minimum sample interval to prevent jitter
        if (elapsed < MIN_SAMPLE_INTERVAL_MS) {
            return currentEps;
        }
        
        // Get counts and reset atomically
        long events = eventCounter.getAndSet(0);
        long errors = errorCounter.getAndSet(0);
        
        // Calculate instant rates
        double instantEps = (events * 1000.0) / elapsed;
        double instantErrorEps = (errors * 1000.0) / elapsed;
        
        // Apply Exponential Moving Average for smoothing
        // EMA = α * new_value + (1 - α) * old_value
        currentEps = (EMA_ALPHA * instantEps) + ((1 - EMA_ALPHA) * currentEps);
        currentErrorEps = (EMA_ALPHA * instantErrorEps) + ((1 - EMA_ALPHA) * currentErrorEps);
        
        lastSampleTime = now;
        
        if (log.isDebugEnabled()) {
            log.debug("Event rate sampled: instant={} eps, smoothed={} eps, errors={} eps",
                    String.format("%.2f", instantEps),
                    String.format("%.2f", currentEps),
                    String.format("%.2f", currentErrorEps));
        }
        
        return currentEps;
    }

    /**
     * Get the current smoothed events per second.
     * Does NOT sample - returns the last calculated value.
     * 
     * @return Current EPS (may be stale if sampleAndGetEps hasn't been called recently)
     */
    public double getCurrentEps() {
        return currentEps;
    }
    
    /**
     * Get the current smoothed error events per second.
     * 
     * @return Current error EPS
     */
    public double getCurrentErrorEps() {
        return currentErrorEps;
    }

    /**
     * Get the number of events waiting to be sampled.
     * Useful for debugging and monitoring.
     * 
     * @return Number of events since last sample
     */
    public long getPendingEventCount() {
        return eventCounter.get();
    }
    
    /**
     * Get the traffic tier name based on current EPS.
     * Useful for logging and dashboards.
     * 
     * @param idleThreshold EPS below which is considered IDLE
     * @param lowThreshold EPS below which is considered LOW
     * @param mediumThreshold EPS below which is considered MEDIUM
     * @param highThreshold EPS below which is considered HIGH
     * @return Tier name: IDLE, LOW, MEDIUM, HIGH, or BURST
     */
    public String getCurrentTier(double idleThreshold, double lowThreshold, 
                                  double mediumThreshold, double highThreshold) {
        if (currentEps < idleThreshold) return "IDLE";
        if (currentEps < lowThreshold) return "LOW";
        if (currentEps < mediumThreshold) return "MEDIUM";
        if (currentEps < highThreshold) return "HIGH";
        return "BURST";
    }
    
    /**
     * Get time since last sample in milliseconds.
     * 
     * @return Milliseconds since last sample
     */
    public long getTimeSinceLastSampleMs() {
        return System.currentTimeMillis() - lastSampleTime;
    }
    
    /**
     * Reset all counters and rates.
     * Useful for testing or when restarting the system.
     */
    public void reset() {
        eventCounter.set(0);
        errorCounter.set(0);
        currentEps = 0.0;
        currentErrorEps = 0.0;
        lastSampleTime = System.currentTimeMillis();
        log.info("EventRateMonitor reset");
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

## Step 3: Understanding the Component

### How It Works

```
┌─────────────────────────────────────────────────────────────────┐
│                    EventRateMonitor Flow                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Event Arrives (from Kafka)                                     │
│         │                                                        │
│         ▼                                                        │
│   recordEvent(isError)                                           │
│         │                                                        │
│         ▼                                                        │
│   eventCounter.incrementAndGet()  ← O(1) atomic operation       │
│                                                                  │
│   ─────────────────────────────────────────────────────────────  │
│                                                                  │
│   Every Tick (called by AdaptiveRuleEvaluator)                   │
│         │                                                        │
│         ▼                                                        │
│   sampleAndGetEps()                                              │
│         │                                                        │
│         ├─► Calculate instant EPS from counter / elapsed time   │
│         │                                                        │
│         ├─► Apply EMA smoothing: new = 0.3*instant + 0.7*old    │
│         │                                                        │
│         ├─► Reset counters to 0                                  │
│         │                                                        │
│         └─► Return smoothed EPS                                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Why Exponential Moving Average?

Without smoothing, a single burst of events would cause rapid interval changes:

```
Time    Events    Instant EPS    Without EMA    With EMA (α=0.3)
─────────────────────────────────────────────────────────────────
T+0s    0         0              0              0
T+1s    100       100            100 → BURST    30 → MEDIUM
T+2s    2         2              2 → LOW        21 → MEDIUM
T+3s    0         0              0 → IDLE       15 → MEDIUM
T+4s    50        50             50 → HIGH      25 → MEDIUM
```

EMA prevents "interval flickering" caused by natural traffic variance.

---

## Step 4: Quick Test (Optional)

Add this temporary test in any `@PostConstruct` method:

```java
@Autowired
private EventRateMonitor rateMonitor;

@PostConstruct
public void testRateMonitor() {
    // Simulate 100 events
    for (int i = 0; i < 100; i++) {
        rateMonitor.recordEvent(i % 10 == 0); // Every 10th is an error
    }
    
    // Wait a bit and sample
    try { Thread.sleep(200); } catch (Exception e) {}
    
    double eps = rateMonitor.sampleAndGetEps();
    System.out.println("=== EventRateMonitor Test ===");
    System.out.println("Simulated 100 events");
    System.out.println("Calculated EPS: " + eps);
    System.out.println("Pending events: " + rateMonitor.getPendingEventCount());
    System.out.println("============================");
}
```

---

## Directory Structure After Phase 2

```
src/main/java/com/eventara/rule/evaluation/
├── config/
│   └── AdaptiveEvaluationProperties.java   ← Phase 1
├── EventRateMonitor.java                   ← NEW (Phase 2)
├── RealTimeRuleEvaluator.java              ← Existing
└── ThresholdState.java                     ← Existing
```

---

## API Reference

| Method | Description | Thread-Safe | Complexity |
|--------|-------------|-------------|------------|
| `recordEvent(boolean isError)` | Record an event arrival | ✅ | O(1) |
| `recordEvent()` | Record an event (no error flag) | ✅ | O(1) |
| `sampleAndGetEps()` | Calculate and return smoothed EPS | ✅ | O(1) |
| `getCurrentEps()` | Get last calculated EPS | ✅ | O(1) |
| `getCurrentErrorEps()` | Get last calculated error EPS | ✅ | O(1) |
| `getPendingEventCount()` | Get events since last sample | ✅ | O(1) |
| `reset()` | Reset all counters | ✅ | O(1) |

---

## ✅ Phase 2 Complete!

You have successfully:
- [x] Created `EventRateMonitor.java`
- [x] Verified the build compiles

---

## Next Step

Proceed to **Phase 3: Evaluation Key** to create the rule grouping mechanism that reduces Redis calls.

---

*Phase 2 Complete | Total Est. Time: 45 minutes*
