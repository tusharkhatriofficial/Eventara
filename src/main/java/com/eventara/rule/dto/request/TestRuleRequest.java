package com.eventara.rule.dto.request;

import com.eventara.alert.enums.AlertSeverity;
import com.eventara.rule.enums.RuleType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRuleRequest {

    @NotNull(message = "Rule type is required")
    private RuleType ruleType;

    @NotNull(message = "Rule configuration is required")
    private Map<String, Object> ruleConfig;

    @NotNull(message = "Severity is required")
    private AlertSeverity severity;

    private Integer priority;

    private String name; // Optional for test
}
