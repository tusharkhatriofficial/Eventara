package com.eventara.rule.evaluation.handler;

import com.eventara.metrics.model.MetricsBucket;
import com.eventara.rule.entity.AlertRule;
import com.eventara.rule.evaluation.model.EvaluationResult;

import java.util.Optional;

/**
 * Strategy interface for rule evaluation handlers.
 * Each handler specializes in evaluating a specific type of rule.
 * 
 * Implementations are auto-discovered by Spring and registered in
 * HandlerRegistry.
 */
public interface RuleHandler {

    /**
     * Check if this handler can process the given rule.
     * 
     * @param rule The rule to check
     * @return true if this handler can evaluate the rule
     */
    boolean canHandle(AlertRule rule);

    /**
     * Evaluate the rule against the provided metrics bucket.
     * 
     * @param rule          The rule to evaluate
     * @param bucket        Pre-fetched metrics from Redis
     * @param windowMinutes The time window for evaluation
     * @return Optional containing EvaluationResult if threshold crossed, empty
     *         otherwise
     */
    Optional<EvaluationResult> evaluate(AlertRule rule, MetricsBucket bucket, int windowMinutes);

    /**
     * Get the priority of this handler (lower = higher priority).
     * Used when multiple handlers could handle the same rule.
     * Default is 100.
     */
    default int getPriority() {
        return 100;
    }
}
