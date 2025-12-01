package com.eventara.rule.dto.request;

import com.eventara.alert.enums.AlertSeverity;
import com.eventara.rule.enums.RuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRuleRequest {

    private String name;

    private String description;

    private RuleType ruleType;

    private Map<String, Object> ruleConfig;

    private AlertSeverity severity;

    private Integer priority;

    private String[] notificationChannels;

    private Map<String, Object> notificationConfig;

    private Integer suppressionWindowMinutes;

    private Integer maxAlertsPerHour;
}
