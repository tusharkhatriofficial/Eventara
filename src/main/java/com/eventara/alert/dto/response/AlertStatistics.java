package com.eventara.alert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatistics {

    private Long totalAlerts;

    private Long activeAlerts;

    private Long acknowledgedAlerts;

    private Long resolvedAlerts;

    private Long criticalAlerts;

    private Long warningAlerts;

    private Long infoAlerts;

    private Map<String, Long> alertsBySeverity;

    private Map<String, Long> alertsByStatus;

    private Map<String, Long> alertsByRule;

    private Double averageResolutionTimeMinutes;
}
