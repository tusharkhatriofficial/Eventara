package com.eventara.rule.dto.response;

import com.eventara.alert.enums.AlertSeverity;
import com.eventara.rule.enums.RuleStatus;
import com.eventara.rule.enums.RuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResponse {

    private Long id;

    private String name;

    private String description;

    private RuleType ruleType;

    private RuleStatus status;

    private Map<String, Object> ruleConfig;

    private AlertSeverity severity;

    private Integer priority;

    private String[] notificationChannels;

    private Map<String, Object> notificationConfig;

    private Integer suppressionWindowMinutes;

    private Integer maxAlertsPerHour;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastTriggeredAt;

    private Integer triggerCount;

    private Integer version;
}
