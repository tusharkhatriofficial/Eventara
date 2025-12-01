package com.eventara.notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationChannelRequest {

    private String name;

    private String description;

    private Map<String, Object> config;

    private Boolean enabled;

    private Integer rateLimitPerMinute;

    private Integer rateLimitPerHour;
}
