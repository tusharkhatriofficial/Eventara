package com.eventara.alert.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcknowledgeAlertRequest {

    @NotBlank(message = "Acknowledged by is required")
    private String acknowledgedBy;

    private String acknowledgmentNotes;
}
