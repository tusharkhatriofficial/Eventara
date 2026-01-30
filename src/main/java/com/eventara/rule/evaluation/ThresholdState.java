package com.eventara.rule.evaluation;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory state for a threshold rule.
 * Tracks events within a sliding window for instant evaluation.
 */
@Data
@Slf4j
public class ThresholdState {

    private final Long ruleId;
    private final int windowMinutes;

    // Sliding window of recent events
    private final Deque<EventSnapshot> recentEvents = new ConcurrentLinkedDeque<>();

    // Last alert time (for cooldown)
    private volatile Instant lastAlertTime;

    public ThresholdState(Long ruleId, int windowMinutes) {
        this.ruleId = ruleId;
        this.windowMinutes = windowMinutes;
    }

    /**
     * Record an event in the sliding window.
     */
    public synchronized void recordEvent(String source, String eventType, String severity,
            boolean isError, long latencyMs) {
        // Trim old events first
        trimOldEvents();

        // Add new event
        recentEvents.addLast(new EventSnapshot(
                Instant.now(), source, eventType, severity, isError, latencyMs));
    }

    /**
     * Remove events outside the time window.
     */
    private void trimOldEvents() {
        Instant cutoff = Instant.now().minusSeconds(windowMinutes * 60L);
        while (!recentEvents.isEmpty() && recentEvents.peekFirst().getTimestamp().isBefore(cutoff)) {
            recentEvents.pollFirst();
        }
    }

    /**
     * Get total event count in window (optionally filtered).
     */
    public long getEventCount(String sourceFilter, String eventTypeFilter) {
        trimOldEvents();
        return recentEvents.stream()
                .filter(e -> matchesFilters(e, sourceFilter, eventTypeFilter))
                .count();
    }

    /**
     * Get error count in window (optionally filtered).
     */
    public long getErrorCount(String sourceFilter, String eventTypeFilter) {
        trimOldEvents();
        return recentEvents.stream()
                .filter(e -> matchesFilters(e, sourceFilter, eventTypeFilter))
                .filter(EventSnapshot::isError)
                .count();
    }

    /**
     * Get error rate in window (optionally filtered).
     */
    public double getErrorRate(String sourceFilter, String eventTypeFilter) {
        long total = getEventCount(sourceFilter, eventTypeFilter);
        if (total == 0)
            return 0.0;
        long errors = getErrorCount(sourceFilter, eventTypeFilter);
        return (errors * 100.0) / total;
    }

    /**
     * Get average latency in window (optionally filtered).
     */
    public double getAvgLatency(String sourceFilter, String eventTypeFilter) {
        trimOldEvents();
        return recentEvents.stream()
                .filter(e -> matchesFilters(e, sourceFilter, eventTypeFilter))
                .mapToLong(EventSnapshot::getLatencyMs)
                .average()
                .orElse(0.0);
    }

    /**
     * Get events per minute in window (optionally filtered).
     */
    public double getEventsPerMinute(String sourceFilter, String eventTypeFilter) {
        long count = getEventCount(sourceFilter, eventTypeFilter);
        return count / (double) Math.max(1, windowMinutes);
    }

    /**
     * Check if currently in cooldown period.
     */
    public boolean isInCooldown(int cooldownMinutes) {
        if (lastAlertTime == null)
            return false;
        return lastAlertTime.plusSeconds(cooldownMinutes * 60L).isAfter(Instant.now());
    }

    /**
     * Mark that an alert was fired (for cooldown tracking).
     */
    public void markAlertFired() {
        this.lastAlertTime = Instant.now();
    }

    private boolean matchesFilters(EventSnapshot event, String sourceFilter, String eventTypeFilter) {
        if (sourceFilter != null && !sourceFilter.isEmpty()
                && !event.getSource().equals(sourceFilter)) {
            return false;
        }
        if (eventTypeFilter != null && !eventTypeFilter.isEmpty()
                && !event.getEventType().equals(eventTypeFilter)) {
            return false;
        }
        return true;
    }

    /**
     * Snapshot of an event for sliding window tracking.
     */
    @Data
    public static class EventSnapshot {
        private final Instant timestamp;
        private final String source;
        private final String eventType;
        private final String severity;
        private final boolean error;
        private final long latencyMs;

        public EventSnapshot(Instant timestamp, String source, String eventType,
                String severity, boolean error, long latencyMs) {
            this.timestamp = timestamp;
            this.source = source;
            this.eventType = eventType;
            this.severity = severity;
            this.error = error;
            this.latencyMs = latencyMs;
        }
    }
}
