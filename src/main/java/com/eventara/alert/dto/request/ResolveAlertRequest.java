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
public class ResolveAlertRequest {

    @NotBlank(message = "Resolved by is required")
    private String resolvedBy;

    @NotBlank(message = "Resolution notes are required")
    private String resolutionNotes;

    private String resolutionType; // MANUAL, AUTO, TIMEOUT
}
