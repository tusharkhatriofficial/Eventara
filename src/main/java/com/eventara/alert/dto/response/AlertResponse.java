package com.eventara.alert.dto.response;

import com.eventara.alert.enums.AlertSeverity;
import com.eventara.alert.enums.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {

    private Long id;

    private Long ruleId;

    private String ruleName;

    private Integer ruleVersion;

    private AlertSeverity severity;

    private Integer priority;

    private String message;

    private String description;

    private Double thresholdValue;

    private Double actualValue;

    private AlertStatus status;

    private LocalDateTime triggeredAt;

    private LocalDateTime acknowledgedAt;

    private String acknowledgedBy;

    private String acknowledgmentNotes;

    private LocalDateTime resolvedAt;

    private String resolvedBy;

    private String resolutionNotes;

    private String resolutionType;

    private Map<String, Object> context;

    private List<Map<String, Object>> notificationsSent;

    private List<String> tags;

    private Integer evaluationTimeMs;
}
