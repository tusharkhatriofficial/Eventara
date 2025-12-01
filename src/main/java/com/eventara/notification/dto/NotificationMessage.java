package com.eventara.notification.dto;

import com.eventara.alert.enums.AlertSeverity;
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
public class NotificationMessage {

    private Long alertId;

    private String ruleName;

    private AlertSeverity severity;

    private String subject;

    private String message;

    private String recipient;

    private LocalDateTime triggeredAt;

    private Double thresholdValue;

    private Double actualValue;

    private Map<String, Object> context;
}
