package com.eventara.alert.service;

import com.eventara.alert.entity.AlertHistory;
import com.eventara.alert.enums.AlertSeverity;
import com.eventara.alert.enums.AlertStatus;
import com.eventara.alert.repository.AlertHistoryRepository;
import com.eventara.rule.entity.AlertRule;
import com.eventara.rule.repository.RuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to handle alert triggering from Drools rules
 */
@Service
@Slf4j
public class AlertTriggerHandler {

    private final AlertHistoryRepository alertHistoryRepository;
    private final RuleRepository ruleRepository;

    @Autowired
    public AlertTriggerHandler(
            AlertHistoryRepository alertHistoryRepository,
            RuleRepository ruleRepository) {
        this.alertHistoryRepository = alertHistoryRepository;
        this.ruleRepository = ruleRepository;
    }

    /**
     * Handle threshold alert triggered by Drools rule
     */
    @Transactional
    public void handleThresholdAlert(
            Long ruleId,
            String ruleName,
            String severity,
            Double thresholdValue,
            Double actualValue) {

        log.info("Alert triggered - Rule: {}, Severity: {}, Threshold: {}, Actual: {}",
                ruleName, severity, thresholdValue, actualValue);

        // Check if we should suppress this alert (already triggered recently)
        if (ruleId != null && shouldSuppressAlert(ruleId)) {
            log.info("Alert suppressed for rule: {}", ruleName);
            return;
        }

        // Create alert history
        AlertHistory alert = AlertHistory.builder()
                .ruleId(ruleId)
                .ruleName(ruleName)
                .ruleVersion(1)
                .severity(AlertSeverity.valueOf(severity))
                .priority(getSeverityPriority(severity))
                .message(buildAlertMessage(ruleName, thresholdValue, actualValue))
                .description("Threshold exceeded for " + ruleName)
                .thresholdValue(thresholdValue)
                .actualValue(actualValue)
                .status(AlertStatus.ACTIVE)
                .triggeredAt(LocalDateTime.now())
                .context(buildContext(ruleName, thresholdValue, actualValue))
                .build();

        alertHistoryRepository.save(alert);

        // Update rule trigger count
        if (ruleId != null) {
            updateRuleTriggerCount(ruleId);
        }

        log.info("Alert saved with ID: {}", alert.getId());
    }

    /**
     * Check if alert should be suppressed based on suppression window
     */
    private boolean shouldSuppressAlert(Long ruleId) {
        AlertRule rule = ruleRepository.findById(ruleId).orElse(null);
        if (rule == null || rule.getSuppressionWindowMinutes() == null) {
            return false;
        }

        LocalDateTime suppressionThreshold = LocalDateTime.now()
                .minusMinutes(rule.getSuppressionWindowMinutes());

        return alertHistoryRepository.hasActiveAlertsInTimeWindow(ruleId, suppressionThreshold);
    }

    /**
     * Update rule trigger count
     */
    private void updateRuleTriggerCount(Long ruleId) {
        AlertRule rule = ruleRepository.findById(ruleId).orElse(null);
        if (rule != null) {
            rule.setTriggerCount(rule.getTriggerCount() + 1);
            rule.setLastTriggeredAt(LocalDateTime.now());
            ruleRepository.save(rule);
        }
    }

    /**
     * Build alert message
     */
    private String buildAlertMessage(String ruleName, Double threshold, Double actualValue) {
        return String.format("Alert: %s - Threshold: %.2f, Actual: %.2f",
                ruleName, threshold, actualValue);
    }

    /**
     * Build alert context
     */
    private Map<String, Object> buildContext(String ruleName, Double threshold, Double actual) {
        Map<String, Object> context = new HashMap<>();
        context.put("ruleName", ruleName);
        context.put("thresholdValue", threshold);
        context.put("actualValue", actual);
        context.put("timestamp", LocalDateTime.now().toString());
        return context;
    }

    /**
     * Get priority based on severity
     */
    private Integer getSeverityPriority(String severity) {
        switch (AlertSeverity.valueOf(severity)) {
            case CRITICAL:
                return 10;
            case WARNING:
                return 5;
            case INFO:
                return 0;
            default:
                return 0;
        }
    }
}
