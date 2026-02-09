package com.eventara.rule.evaluation.handler;

import com.eventara.metrics.model.MetricsBucket;
import com.eventara.rule.entity.AlertRule;
import com.eventara.rule.evaluation.model.EvaluationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handler for composite rules with AND/OR operators.
 * Evaluates multiple conditions and combines results.
 * 
 * Example rule config:
 * {
 * "operator": "AND",
 * "conditions": [
 * {"metricType": "ERROR_RATE", "condition": "GREATER_THAN", "value": 5.0},
 * {"metricType": "AVG_LATENCY", "condition": "GREATER_THAN", "value": 500}
 * ],
 * "sourceFilter": ["payment-service"]
 * }
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CompositeRuleHandler implements RuleHandler {

    private final SimpleThresholdHandler simpleHandler; // Reuse metric extraction logic

    @Override
    public boolean canHandle(AlertRule rule) {
        Map<String, Object> config = rule.getRuleConfig();
        return config != null && config.containsKey("conditions");
    }

    @Override
    public int getPriority() {
        return 50; // Higher priority than simple (lower number = higher priority)
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<EvaluationResult> evaluate(AlertRule rule, MetricsBucket bucket, int windowMinutes) {
        Map<String, Object> config = rule.getRuleConfig();

        List<Map<String, Object>> conditions = (List<Map<String, Object>>) config.get("conditions");
        String operator = (String) config.getOrDefault("operator", "AND");
        int minEvents = simpleHandler.parseIntOrDefault(config.get("minEventsToEvaluate"), 1);

        if (conditions == null || conditions.isEmpty()) {
            log.warn("Composite rule {} has no conditions", rule.getId());
            return Optional.empty();
        }

        // Check minimum events
        if (bucket.getTotalEvents() < minEvents) {
            return Optional.empty();
        }

        boolean isAnd = "AND".equalsIgnoreCase(operator);
        boolean result = isAnd; // AND starts true, OR starts false

        StringBuilder details = new StringBuilder();
        details.append("COMPOSITE[").append(operator).append("]: ");

        double primaryValue = 0;
        double primaryThreshold = 0;
        boolean first = true;
        int metCount = 0;

        for (Map<String, Object> cond : conditions) {
            String metricType = (String) cond.get("metricType");
            String condOp = (String) cond.get("condition");
            double threshold = simpleHandler.parseDouble(cond.get("value"));

            double currentValue = simpleHandler.getMetricValue(metricType, bucket, windowMinutes);
            boolean conditionMet = simpleHandler.isThresholdCrossed(condOp, currentValue, threshold);

            log.info("Composite condition: {} = {} {} {} -> {}",
                    metricType, currentValue, condOp, threshold, conditionMet ? "MET" : "NOT MET");

            if (conditionMet)
                metCount++;

            // Track primary values for alert message
            if (first) {
                primaryValue = currentValue;
                primaryThreshold = threshold;
                first = false;
            }

            // Build details string
            if (details.length() > 20) { // After "COMPOSITE[AND]: "
                details.append(" | ");
            }
            details.append(String.format("%s(%.2f) %s %.2f %s",
                    metricType, currentValue, condSymbol(condOp), threshold,
                    conditionMet ? "✓" : "✗"));

            // Apply operator logic
            if (isAnd) {
                result = result && conditionMet;
            } else {
                result = result || conditionMet;
            }
        }

        log.info("Composite [{}] evaluation: {}/{} conditions met -> {}",
                operator, metCount, conditions.size(), result ? "TRIGGER" : "no trigger");

        if (result) {
            return Optional.of(new EvaluationResult(primaryValue, primaryThreshold, details.toString()));
        }

        return Optional.empty();
    }

    private String condSymbol(String condition) {
        switch (condition) {
            case "GREATER_THAN":
                return ">";
            case "GREATER_THAN_OR_EQUAL":
                return ">=";
            case "LESS_THAN":
                return "<";
            case "LESS_THAN_OR_EQUAL":
                return "<=";
            case "EQUALS":
                return "==";
            case "NOT_EQUALS":
                return "!=";
            default:
                return condition;
        }
    }
}
