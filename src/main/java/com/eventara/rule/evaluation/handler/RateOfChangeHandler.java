package com.eventara.rule.evaluation.handler;

import com.eventara.metrics.model.MetricsBucket;
import com.eventara.metrics.service.RedisMetricsService;
import com.eventara.rule.entity.AlertRule;
import com.eventara.rule.evaluation.model.EvaluationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Handler for Rate of Change rules (spike detection).
 * Compares current metric value to previous time window.
 * 
 * Example: Detect error rate spike
 * {
 * "metricType": "ERROR_RATE_CHANGE",
 * "condition": "GREATER_THAN",
 * "thresholdValue": 50.0, // 50% increase
 * "timeWindowMinutes": 5,
 * "sourceFilter": ["payment-service"]
 * }
 * 
 * Supported metrics:
 * - ERROR_RATE_CHANGE: % change in error rate
 * - LATENCY_CHANGE: % change in avg latency
 * - THROUGHPUT_CHANGE: % change in events per minute
 * - SPIKE_DETECTION: % change in total events
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RateOfChangeHandler implements RuleHandler {

    private final RedisMetricsService redisMetrics;
    private final SimpleThresholdHandler simpleHandler;

    private static final Set<String> CHANGE_METRICS = Set.of(
            "ERROR_RATE_CHANGE", "LATENCY_CHANGE", "THROUGHPUT_CHANGE", "SPIKE_DETECTION");

    @Override
    public boolean canHandle(AlertRule rule) {
        Map<String, Object> config = rule.getRuleConfig();
        if (config == null)
            return false;

        String metricType = (String) config.get("metricType");
        return metricType != null && CHANGE_METRICS.contains(metricType);
    }

    @Override
    public int getPriority() {
        return 70; // Medium-low priority
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<EvaluationResult> evaluate(AlertRule rule, MetricsBucket currentBucket, int windowMinutes) {
        Map<String, Object> config = rule.getRuleConfig();

        String metricType = (String) config.get("metricType");
        String condition = (String) config.get("condition");
        double threshold = simpleHandler.parseDouble(config.get("thresholdValue"));

        if (condition == null) {
            log.warn("Rate of change rule {} has no condition", rule.getId());
            return Optional.empty();
        }

        // Get PREVIOUS window bucket for comparison
        MetricsBucket previousBucket = fetchPreviousBucket(config, windowMinutes);

        // Get the base metric type
        String baseMetric = getBaseMetric(metricType);
        double currentValue = simpleHandler.getMetricValue(baseMetric, currentBucket, windowMinutes);
        double previousValue = simpleHandler.getMetricValue(baseMetric, previousBucket, windowMinutes);

        // Calculate % change
        double percentChange = calculatePercentChange(currentValue, previousValue);

        boolean crossed = simpleHandler.isThresholdCrossed(condition, percentChange, threshold);

        if (crossed) {
            String arrow = percentChange >= 0 ? "↑" : "↓";
            String details;
            if (previousValue == 0 && currentValue > 0) {
                // Spike from zero - show cleaner message
                details = String.format("%s: SPIKE DETECTED (0 → %.2f) - new errors appeared",
                        metricType, currentValue);
            } else {
                details = String.format("%s: %.1f%% %s (%.2f → %.2f) %s %.1f%%",
                        metricType, Math.abs(percentChange), arrow,
                        previousValue, currentValue,
                        condSymbol(condition), threshold);
            }

            log.info("Rule {} RATE_OF_CHANGE triggered: {}", rule.getName(), details);
            return Optional.of(new EvaluationResult(percentChange, threshold, details));
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private MetricsBucket fetchPreviousBucket(Map<String, Object> config, int windowMinutes) {
        // Check for source filter
        if (config.containsKey("sourceFilter")) {
            List<String> sources = (List<String>) config.get("sourceFilter");
            if (sources != null && !sources.isEmpty()) {
                return redisMetrics.getMetricsForSourcePreviousWindow(sources.get(0), windowMinutes);
            }
        }

        // Check for event type filter
        if (config.containsKey("eventTypeFilter")) {
            List<String> types = (List<String>) config.get("eventTypeFilter");
            if (types != null && !types.isEmpty()) {
                return redisMetrics.getMetricsForEventTypePreviousWindow(types.get(0), windowMinutes);
            }
        }

        // Global previous window
        return redisMetrics.getMetricsPreviousWindow(windowMinutes);
    }

    private double calculatePercentChange(double current, double previous) {
        if (previous == 0) {
            // Handle zero previous value - return very large % change for spike detection
            // 999% ensures GREATER_THAN thresholds like > 100% will trigger
            return current > 0 ? 999.0 : 0;
        }
        return ((current - previous) / previous) * 100.0;
    }

    private String getBaseMetric(String changeMetric) {
        switch (changeMetric) {
            case "ERROR_RATE_CHANGE":
                return "ERROR_RATE";
            case "LATENCY_CHANGE":
                return "AVG_LATENCY";
            case "THROUGHPUT_CHANGE":
                return "EVENTS_PER_MINUTE";
            case "SPIKE_DETECTION":
                return "TOTAL_EVENTS";
            default:
                return "ERROR_RATE";
        }
    }

    private String condSymbol(String condition) {
        switch (condition) {
            case "GREATER_THAN":
                return ">";
            case "LESS_THAN":
                return "<";
            default:
                return condition;
        }
    }
}
