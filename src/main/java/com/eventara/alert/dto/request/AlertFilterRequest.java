package com.eventara.alert.dto.request;

import com.eventara.alert.enums.AlertSeverity;
import com.eventara.alert.enums.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertFilterRequest {

    private AlertStatus status;

    private AlertSeverity severity;

    private Long ruleId;

    private String ruleName;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String searchTerm;

    private Integer page;

    private Integer size;

    private String sortBy;

    private String sortDirection;
}
