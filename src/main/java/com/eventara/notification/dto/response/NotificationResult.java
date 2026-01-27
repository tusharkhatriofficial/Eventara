package com.eventara.notification.dto.response;

import com.eventara.notification.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResult {

    private Long id;

    private Long alertId;

    private Long channelId;

    private String channelName;

    private String channelType;

    private String recipient;

    private NotificationStatus status;

    private String message;

    private LocalDateTime sentAt;

    private Integer responseCode;

    private String responseBody;

    private Integer retryCount;

    private Integer deliveryTimeMs;

    private String errorMessage;
}
