package com.eventara.notification.dto.request;

import com.eventara.notification.enums.ChannelType;
import jakarta.validation.constraints.NotBlank;
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
public class CreateNotificationChannelRequest {

    @NotNull(message = "Channel type is required")
    private ChannelType channelType;

    @NotBlank(message = "Channel name is required")
    private String name;

    private String description;

    @NotNull(message = "Configuration is required")
    private Map<String, Object> config;

    private Integer rateLimitPerMinute;

    private Integer rateLimitPerHour;

    private String createdBy;
}
