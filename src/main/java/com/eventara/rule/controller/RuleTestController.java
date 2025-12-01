package com.eventara.rule.controller;

import com.eventara.rule.dto.request.TestRuleRequest;
import com.eventara.rule.dto.response.RuleTestResult;
import com.eventara.rule.service.RuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rules/test")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RuleTestController {

    private final RuleService ruleService;

    /**
     * Test a rule configuration without saving it
     * POST /api/v1/rules/test
     */
    @PostMapping
    public ResponseEntity<RuleTestResult> testRule(@Valid @RequestBody TestRuleRequest request) {
        log.info("Testing rule configuration");
        RuleTestResult result = ruleService.testRule(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Test an existing rule by ID
     * POST /api/v1/rules/test/{id}
     */
    @PostMapping("/{id}")
    public ResponseEntity<RuleTestResult> testRuleById(@PathVariable Long id) {
        log.info("Testing existing rule with ID: {}", id);
        RuleTestResult result = ruleService.testRuleById(id);
        return ResponseEntity.ok(result);
    }
}
