package com.eventara.rule.evaluation.handler;

import com.eventara.metrics.model.MetricsBucket;
import com.eventara.rule.entity.AlertRule;
import com.eventara.rule.evaluation.model.EvaluationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Handler for simple threshold rules.
 * Evaluates single metric against a threshold value.
 * 
 * Example rule config:
 * {
 * "metricType": "ERROR_RATE",
 * "condition": "GREATER_THAN",
 * "thresholdValue": 5.0,
 * "timeWindowMinutes": 5
 * }
 */
@Component
@Slf4j
public class SimpleThresholdHandler implements RuleHandler {

    // Metric types that are handled by specialized handlers
    private static final Set<String> SPECIAL_METRICS = Set.of(
            "EVENT_RATIO", "ERROR_RATE_CHANGE", "LATENCY_CHANGE",
            "THROUGHPUT_CHANGE", "SPIKE_DETECTION");

    @Override
    public boolean canHandle(AlertRule rule) {
        Map<String, Object> config = rule.getRuleConfig();
        if (config == null)
            return false;

        // Must have metricType
        String metricType = (String) config.get("metricType");
        if (metricType == null)
            return false;

        // Must NOT have conditions array (that's for CompositeRuleHandler)
        if (config.containsKey("conditions"))
            return false;

        // Must NOT be a special metric type
        return !SPECIAL_METRICS.contains(metricType);
    }

    @Override
    public int getPriority() {
        return 100; // Default priority
    }

    @Override
    public Optional<EvaluationResult> evaluate(AlertRule rule, MetricsBucket bucket, int windowMinutes) {
        Map<String, Object> config = rule.getRuleConfig();

        String metricType = (String) config.get("metricType");
        String condition = (String) config.get("condition");
        double threshold = parseDouble(config.get("thresholdValue"));
        int minEvents = parseIntOrDefault(config.get("minEventsToEvaluate"), 1);

        if (condition == null) {
            log.warn("Rule {} has no condition specified", rule.getId());
            return Optional.empty();
        }

        // Check minimum events requirement
        if (bucket.getTotalEvents() < minEvents) {
            log.debug("Rule {}: Not enough events ({} < {})",
                    rule.getId(), bucket.getTotalEvents(), minEvents);
            return Optional.empty();
        }

        double currentValue = getMetricValue(metricType, bucket, windowMinutes);
        boolean crossed = isThresholdCrossed(condition, currentValue, threshold);

        if (crossed) {
            log.debug("Rule {} crossed threshold: {} {} {}",
                    rule.getName(), currentValue, condition, threshold);
            return Optional.of(EvaluationResult.simple(metricType, currentValue, condition, threshold));
        }

        return Optional.empty();
    }

    /**
     * Extract metric value from bucket based on metric type.
     */
    protected double getMetricValue(String metricType, MetricsBucket bucket, int windowMinutes) {
        if (bucket == null)
            return 0.0;

        switch (metricType) {
            // Error Metrics
            case "ERROR_RATE":
                return bucket.getErrorRate();
            case "TOTAL_ERRORS":
                return bucket.getTotalErrors();

            // Latency Metrics
            case "AVG_LATENCY":
                return bucket.getAvgLatency();
            case "P50_LATENCY":
                return bucket.getLatencyP50() != null ? bucket.getLatencyP50() : 0.0;
            case "P95_LATENCY":
                return bucket.getLatencyP95() != null ? bucket.getLatencyP95() : 0.0;
            case "P99_LATENCY":
                return bucket.getLatencyP99() != null ? bucket.getLatencyP99() : 0.0;
            case "MAX_LATENCY":
                return bucket.getLatencyMax() != null ? bucket.getLatencyMax() : 0.0;
            case "MIN_LATENCY":
                return bucket.getLatencyMin() != null ? bucket.getLatencyMin() : 0.0;

            // Throughput Metrics
            case "TOTAL_EVENTS":
                return bucket.getTotalEvents();
            case "EVENTS_PER_SECOND":
                return bucket.getTotalEvents() / (double) Math.max(1, windowMinutes * 60);
            case "EVENTS_PER_MINUTE":
                return bucket.getTotalEvents() / (double) Math.max(1, windowMinutes);
            case "EVENTS_PER_HOUR":
                return bucket.getTotalEvents() * 60.0 / Math.max(1, windowMinutes);
            case "EVENTS_PER_DAY":
                return bucket.getTotalEvents() * 1440.0 / Math.max(1, windowMinutes);

            // Time Window Metrics (mapped to total events for the bucket)
            case "EVENTS_LAST_1_MINUTE":
            case "EVENTS_LAST_5_MINUTES":
            case "EVENTS_LAST_15_MINUTES":
            case "EVENTS_LAST_1_HOUR":
            case "EVENTS_LAST_24_HOURS":
                return bucket.getTotalEvents();

            // Summary Metrics
            case "UNIQUE_SOURCES":
                return bucket.getUniqueSourcesEstimate() > 0
                        ? bucket.getUniqueSourcesEstimate()
                        : bucket.getBySource().size();
            case "UNIQUE_EVENT_TYPES":
                return bucket.getUniqueEventTypesEstimate() > 0
                        ? bucket.getUniqueEventTypesEstimate()
                        : bucket.getByEventType().size();
            case "UNIQUE_USERS":
                return bucket.getUniqueUsersEstimate();
            case "SYSTEM_HEALTH":
                // Simple health score: 100 - error_rate
                return Math.max(0, 100 - bucket.getErrorRate());

            // User Metrics
            case "ACTIVE_USERS_LAST_1_HOUR":
            case "ACTIVE_USERS_LAST_24_HOURS":
            case "TOTAL_UNIQUE_USERS":
                return bucket.getUniqueUsersEstimate();

            default:
                log.warn("Unknown metric type: {}", metricType);
                return 0.0;
        }
    }

    /**
     * Check if threshold condition is met.
     */
    protected boolean isThresholdCrossed(String condition, double current, double threshold) {
        switch (condition) {
            case "GREATER_THAN":
                return current > threshold;
            case "GREATER_THAN_OR_EQUAL":
                return current >= threshold;
            case "LESS_THAN":
                return current < threshold;
            case "LESS_THAN_OR_EQUAL":
                return current <= threshold;
            case "EQUALS":
                return Math.abs(current - threshold) < 0.0001;
            case "NOT_EQUALS":
                return Math.abs(current - threshold) >= 0.0001;
            default:
                log.warn("Unknown condition: {}", condition);
                return false;
        }
    }

    protected double parseDouble(Object value) {
        if (value == null)
            return 0.0;
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    protected int parseIntOrDefault(Object value, int defaultValue) {
        if (value == null)
            return defaultValue;
        if (value instanceof Number)
            return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
