package com.eventara.rule.evaluation.handler;

import com.eventara.metrics.model.MetricsBucket;
import com.eventara.metrics.service.RedisMetricsService;
import com.eventara.rule.entity.AlertRule;
import com.eventara.rule.evaluation.model.EvaluationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Handler for EVENT_RATIO rules (conversion rates).
 * Compares ratio of one event type to another.
 * 
 * Example: Track checkout conversion rate
 * {
 * "metricType": "EVENT_RATIO",
 * "numeratorEventType": "checkout.success",
 * "denominatorEventType": "checkout.started",
 * "condition": "LESS_THAN",
 * "thresholdValue": 0.8,
 * "minDenominatorEvents": 10
 * }
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EventRatioHandler implements RuleHandler {

    private final RedisMetricsService redisMetrics;
    private final SimpleThresholdHandler simpleHandler;

    @Override
    public boolean canHandle(AlertRule rule) {
        Map<String, Object> config = rule.getRuleConfig();
        if (config == null)
            return false;

        String metricType = (String) config.get("metricType");
        return "EVENT_RATIO".equals(metricType);
    }

    @Override
    public int getPriority() {
        return 60; // Medium priority
    }

    @Override
    public Optional<EvaluationResult> evaluate(AlertRule rule, MetricsBucket bucket, int windowMinutes) {
        Map<String, Object> config = rule.getRuleConfig();

        String numeratorType = (String) config.get("numeratorEventType");
        String denominatorType = (String) config.get("denominatorEventType");
        String condition = (String) config.get("condition");
        double threshold = simpleHandler.parseDouble(config.get("thresholdValue"));
        int minDenominator = simpleHandler.parseIntOrDefault(config.get("minDenominatorEvents"), 5);

        if (numeratorType == null || denominatorType == null || condition == null) {
            log.warn("EVENT_RATIO rule {} missing required fields", rule.getId());
            return Optional.empty();
        }

        // Fetch metrics for BOTH event types
        // This is an exception to "no Redis in handlers" - ratio rules require two
        // buckets
        MetricsBucket numeratorBucket = redisMetrics.getMetricsForEventType(numeratorType, windowMinutes);
        MetricsBucket denominatorBucket = redisMetrics.getMetricsForEventType(denominatorType, windowMinutes);

        long numeratorCount = numeratorBucket.getTotalEvents();
        long denominatorCount = denominatorBucket.getTotalEvents();

        // Check minimum denominator
        if (denominatorCount < minDenominator) {
            log.debug("EVENT_RATIO rule {}: Not enough denominator events ({} < {})",
                    rule.getId(), denominatorCount, minDenominator);
            return Optional.empty();
        }

        // Calculate ratio
        double ratio = (double) numeratorCount / denominatorCount;
        boolean crossed = simpleHandler.isThresholdCrossed(condition, ratio, threshold);

        if (crossed) {
            String details = String.format("EVENT_RATIO: %s/%s = %d/%d = %.3f %s %.3f",
                    numeratorType, denominatorType,
                    numeratorCount, denominatorCount,
                    ratio, condSymbol(condition), threshold);

            log.info("Rule {} EVENT_RATIO triggered: {}", rule.getName(), details);
            return Optional.of(new EvaluationResult(ratio, threshold, details));
        }

        return Optional.empty();
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
