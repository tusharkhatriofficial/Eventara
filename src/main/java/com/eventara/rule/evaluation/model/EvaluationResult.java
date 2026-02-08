package com.eventara.rule.evaluation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of rule evaluation when a threshold is crossed.
 * Contains the values needed to trigger an alert.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvaluationResult {

    /**
     * The current metric value that triggered the rule.
     */
    private double currentValue;

    /**
     * The threshold that was crossed.
     */
    private double threshold;

    /**
     * Human-readable details about the evaluation.
     * Examples:
     * - "ERROR_RATE: 15.5 > 10.0"
     * - "COMPOSITE[AND]: ERROR_RATE(15.5) > 10.0 ✓ | AVG_LATENCY(800) > 500 ✓"
     * - "checkout.success/checkout.started = 45/100 = 0.45"
     */
    private String details;

    /**
     * Create a simple result with auto-generated details.
     */
    public static EvaluationResult simple(String metricType, double currentValue,
            String condition, double threshold) {
        String details = String.format("%s: %.2f %s %.2f",
                metricType, currentValue, conditionSymbol(condition), threshold);
        return new EvaluationResult(currentValue, threshold, details);
    }

    private static String conditionSymbol(String condition) {
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
