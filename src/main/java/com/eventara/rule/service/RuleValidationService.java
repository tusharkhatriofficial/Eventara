package com.eventara.rule.service;

import com.eventara.rule.dto.request.CreateRuleRequest;
import com.eventara.rule.dto.request.UpdateRuleRequest;
import com.eventara.rule.dto.request.TestRuleRequest;
import com.eventara.rule.enums.Condition;
import com.eventara.rule.enums.MetricType;
import com.eventara.rule.exception.InvalidRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RuleValidationService {

    public void validateRuleConfig(CreateRuleRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new InvalidRuleException("Rule name is required");
        }

        if (request.getRuleType() == null) {
            throw new InvalidRuleException("Rule type is required");
        }

        if (request.getSeverity() == null) {
            throw new InvalidRuleException("Severity is required");
        }

        if (request.getRuleConfig() == null || request.getRuleConfig().isEmpty()) {
            throw new InvalidRuleException("Rule configuration is required");
        }

        validateRuleConfigMap(request.getRuleConfig());
    }

    public void validateRuleConfig(UpdateRuleRequest request) {
        if (request.getRuleConfig() != null) {
            validateRuleConfigMap(request.getRuleConfig());
        }
    }

    public void validateRuleConfig(TestRuleRequest request) {
        if (request.getRuleType() == null) {
            throw new InvalidRuleException("Rule type is required");
        }

        if (request.getSeverity() == null) {
            throw new InvalidRuleException("Severity is required");
        }

        if (request.getRuleConfig() == null || request.getRuleConfig().isEmpty()) {
            throw new InvalidRuleException("Rule configuration is required");
        }

        validateRuleConfigMap(request.getRuleConfig());
    }

    private void validateRuleConfigMap(Map<String, Object> config) {
        // Check if this is a composite rule (has conditions array)
        if (config.containsKey("conditions")) {
            validateCompositeRule(config);
            return;
        }

        // Check if this is an event ratio rule (Phase 2)
        if (config.containsKey("metricType") && "EVENT_RATIO".equals(config.get("metricType").toString())) {
            validateEventRatioRule(config);
            return;
        }

        // Otherwise, validate as a simple threshold rule
        validateSimpleThresholdRule(config);
    }

    /**
     * Validate composite rule configuration (AND/OR with multiple conditions).
     */
    @SuppressWarnings("unchecked")
    private void validateCompositeRule(Map<String, Object> config) {
        // Validate operator
        if (!config.containsKey("operator")) {
            throw new InvalidRuleException("operator is required for composite rules");
        }

        String operator = config.get("operator").toString();
        if (!"AND".equalsIgnoreCase(operator) && !"OR".equalsIgnoreCase(operator)) {
            throw new InvalidRuleException("operator must be either 'AND' or 'OR'");
        }

        // Validate conditions array
        Object conditionsObj = config.get("conditions");
        if (!(conditionsObj instanceof List)) {
            throw new InvalidRuleException("conditions must be an array for composite rules");
        }

        List<?> conditionsList = (List<?>) conditionsObj;
        if (conditionsList.isEmpty()) {
            throw new InvalidRuleException("conditions array cannot be empty for composite rules");
        }

        // Validate each condition
        for (int i = 0; i < conditionsList.size(); i++) {
            Object condObj = conditionsList.get(i);
            if (!(condObj instanceof Map)) {
                throw new InvalidRuleException("Each condition must be an object at index " + i);
            }

            Map<String, Object> condition = (Map<String, Object>) condObj;

            // Validate metricType in condition
            if (!condition.containsKey("metricType")) {
                throw new InvalidRuleException("metricType is required in condition at index " + i);
            }

            String metricTypeStr = condition.get("metricType").toString();
            try {
                MetricType.valueOf(metricTypeStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidRuleException(
                        "Invalid metric type '" + metricTypeStr + "' in condition at index " + i);
            }

            // Validate condition operator
            if (!condition.containsKey("condition")) {
                throw new InvalidRuleException("condition is required in condition at index " + i);
            }

            String conditionStr = condition.get("condition").toString();
            try {
                Condition.valueOf(conditionStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidRuleException("Invalid condition '" + conditionStr + "' in condition at index " + i);
            }

            // Validate value (threshold)
            if (!condition.containsKey("value")) {
                throw new InvalidRuleException("value is required in condition at index " + i);
            }

            try {
                Double.parseDouble(condition.get("value").toString());
            } catch (NumberFormatException e) {
                throw new InvalidRuleException("value must be a valid number in condition at index " + i);
            }
        }

        log.debug("Composite rule configuration validated successfully with {} conditions", conditionsList.size());
    }

    /**
     * Validate event ratio rule configuration.
     */
    private void validateEventRatioRule(Map<String, Object> config) {
        // Validate numeratorEventType
        if (!config.containsKey("numeratorEventType")) {
            throw new InvalidRuleException("numeratorEventType is required for EVENT_RATIO rules");
        }

        // Validate denominatorEventType
        if (!config.containsKey("denominatorEventType")) {
            throw new InvalidRuleException("denominatorEventType is required for EVENT_RATIO rules");
        }

        // Validate condition
        if (!config.containsKey("condition")) {
            throw new InvalidRuleException("condition is required for EVENT_RATIO rules");
        }

        String conditionStr = config.get("condition").toString();
        try {
            Condition.valueOf(conditionStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidRuleException("Invalid condition: " + conditionStr);
        }

        // Validate thresholdValue
        if (!config.containsKey("thresholdValue")) {
            throw new InvalidRuleException("thresholdValue is required for EVENT_RATIO rules");
        }

        try {
            Double.parseDouble(config.get("thresholdValue").toString());
        } catch (NumberFormatException e) {
            throw new InvalidRuleException("thresholdValue must be a valid number for EVENT_RATIO rules");
        }

        log.debug("EVENT_RATIO rule configuration validated successfully");
    }

    /**
     * Validate simple threshold rule configuration.
     */
    private void validateSimpleThresholdRule(Map<String, Object> config) {
        // Validate metric type
        if (!config.containsKey("metricType")) {
            throw new InvalidRuleException("metricType is required in rule configuration");
        }

        String metricTypeStr = config.get("metricType").toString();
        MetricType metricType;
        try {
            metricType = MetricType.valueOf(metricTypeStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidRuleException("Invalid metric type: " + metricTypeStr);
        }

        // Validate condition
        if (!config.containsKey("condition")) {
            throw new InvalidRuleException("condition is required in rule configuration");
        }

        String conditionStr = config.get("condition").toString();
        try {
            Condition.valueOf(conditionStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidRuleException("Invalid condition: " + conditionStr);
        }

        // Validate threshold value
        if (!config.containsKey("thresholdValue")) {
            throw new InvalidRuleException("thresholdValue is required in rule configuration");
        }

        // Check if metric type is string-based
        if (isStringMetricType(metricType)) {
            // For string metrics, thresholdValue can be a string
            String thresholdValue = config.get("thresholdValue").toString();
            if (thresholdValue == null || thresholdValue.trim().isEmpty()) {
                throw new InvalidRuleException("thresholdValue cannot be empty for string metrics");
            }
            log.debug("String metric validated: {} = {}", metricType, thresholdValue);
        } else {
            // For numeric metrics, validate as number
            try {
                Double.parseDouble(config.get("thresholdValue").toString());
            } catch (NumberFormatException e) {
                throw new InvalidRuleException("thresholdValue must be a valid number");
            }
        }

        // Validate time window if present
        if (config.containsKey("timeWindowMinutes")) {
            try {
                int timeWindow = Integer.parseInt(config.get("timeWindowMinutes").toString());
                if (timeWindow <= 0) {
                    throw new InvalidRuleException("timeWindowMinutes must be positive");
                }
            } catch (NumberFormatException e) {
                throw new InvalidRuleException("timeWindowMinutes must be a valid integer");
            }
        }

        log.debug("Rule configuration validated successfully");
    }

    /**
     * Check if metric type is string-based
     */
    private boolean isStringMetricType(MetricType metricType) {
        switch (metricType) {
            case SYSTEM_HEALTH:
                return true;
            default:
                return false;
        }
    }
}
