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
     * @param rule          The AlertRule to extract key from
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

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