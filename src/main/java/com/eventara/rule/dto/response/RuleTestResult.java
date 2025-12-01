package com.eventara.rule.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleTestResult {

    private Boolean success;

    private String message;

    private String generatedDrl;

    private List<String> errors;

    private List<String> warnings;
}
