package com.eventara.rule.service;

import com.eventara.rule.dto.request.CreateRuleRequest;
import com.eventara.rule.dto.request.TestRuleRequest;
import com.eventara.rule.dto.request.UpdateRuleRequest;
import com.eventara.rule.enums.Condition;
import com.eventara.rule.enums.MetricType;
import com.eventara.rule.exception.InvalidRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        // Validate metric type
        if (!config.containsKey("metricType")) {
            throw new InvalidRuleException("metricType is required in rule configuration");
        }

        String metricTypeStr = config.get("metricType").toString();
        try {
            MetricType.valueOf(metricTypeStr);
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

        try {
            Double.parseDouble(config.get("thresholdValue").toString());
        } catch (NumberFormatException e) {
            throw new InvalidRuleException("thresholdValue must be a valid number");
        }

        // Validate time window
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
}
