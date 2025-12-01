package com.eventara.rule.dto.request;

import com.eventara.alert.enums.AlertSeverity;
import com.eventara.rule.enums.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateRuleRequest {

    @NotBlank(message = "Rule name is required")
    private String name;

    private String description;

    @NotNull(message = "Rule type is required")
    private RuleType ruleType;

    @NotNull(message = "Rule configuration is required")
    private Map<String, Object> ruleConfig;

    @NotNull(message = "Severity is required")
    private AlertSeverity severity;

    private Integer priority;

    private String[] notificationChannels;

    private Map<String, Object> notificationConfig;

    private Integer suppressionWindowMinutes;

    private Integer maxAlertsPerHour;

    private String createdBy;

}
