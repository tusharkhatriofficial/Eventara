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
     * @return Current EPS (may be stale if sampleAndGetEps hasn't been called
     *         recently)
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
     * @param idleThreshold   EPS below which is considered IDLE
     * @param lowThreshold    EPS below which is considered LOW
     * @param mediumThreshold EPS below which is considered MEDIUM
     * @param highThreshold   EPS below which is considered HIGH
     * @return Tier name: IDLE, LOW, MEDIUM, HIGH, or BURST
     */
    public String getCurrentTier(double idleThreshold, double lowThreshold,
            double mediumThreshold, double highThreshold) {
        if (currentEps < idleThreshold)
            return "IDLE";
        if (currentEps < lowThreshold)
            return "LOW";
        if (currentEps < mediumThreshold)
            return "MEDIUM";
        if (currentEps < highThreshold)
            return "HIGH";
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