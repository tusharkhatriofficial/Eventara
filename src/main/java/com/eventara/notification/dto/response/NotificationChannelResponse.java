package com.eventara.notification.dto.response;

import com.eventara.notification.enums.ChannelType;
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
public class NotificationChannelResponse {

    private Long id;

    private ChannelType channelType;

    private String name;

    private String description;

    private Boolean enabled;

    private Map<String, Object> config; // Sensitive data should be masked

    private Integer rateLimitPerMinute;

    private Integer rateLimitPerHour;

    private LocalDateTime lastUsedAt;

    private Integer totalSent;

    private Integer totalFailed;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;
}
