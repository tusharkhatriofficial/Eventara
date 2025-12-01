package com.eventara.rule.controller;

import com.eventara.rule.dto.request.CreateRuleRequest;
import com.eventara.rule.dto.request.UpdateRuleRequest;
import com.eventara.rule.dto.response.RuleResponse;
import com.eventara.rule.enums.RuleStatus;
import com.eventara.rule.enums.RuleType;
import com.eventara.rule.service.RuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RuleController {

    private final RuleService ruleService;

    /**
     * Create a new rule
     * POST /api/v1/rules
     */
    @PostMapping
    public ResponseEntity<RuleResponse> createRule(@Valid @RequestBody CreateRuleRequest request) {
        log.info("Creating new rule: {}", request.getName());
        RuleResponse response = ruleService.createRule(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all rules
     * GET /api/v1/rules
     */
    @GetMapping
    public ResponseEntity<List<RuleResponse>> getAllRules() {
        log.info("Fetching all rules");
        List<RuleResponse> rules = ruleService.getAllRules();
        return ResponseEntity.ok(rules);
    }

    /**
     * Get rule by ID
     * GET /api/v1/rules/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<RuleResponse> getRuleById(@PathVariable Long id) {
        log.info("Fetching rule with ID: {}", id);
        RuleResponse response = ruleService.getRuleById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update rule
     * PUT /api/v1/rules/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<RuleResponse> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRuleRequest request) {
        log.info("Updating rule with ID: {}", id);
        RuleResponse response = ruleService.updateRule(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete rule
     * DELETE /api/v1/rules/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        log.info("Deleting rule with ID: {}", id);
        ruleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Enable rule
     * POST /api/v1/rules/{id}/enable
     */
    @PostMapping("/{id}/enable")
    public ResponseEntity<RuleResponse> enableRule(@PathVariable Long id) {
        log.info("Enabling rule with ID: {}", id);
        RuleResponse response = ruleService.enableRule(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Disable rule
     * POST /api/v1/rules/{id}/disable
     */
    @PostMapping("/{id}/disable")
    public ResponseEntity<RuleResponse> disableRule(@PathVariable Long id) {
        log.info("Disabling rule with ID: {}", id);
        RuleResponse response = ruleService.disableRule(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Archive rule
     * POST /api/v1/rules/{id}/archive
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<RuleResponse> archiveRule(@PathVariable Long id) {
        log.info("Archiving rule with ID: {}", id);
        RuleResponse response = ruleService.archiveRule(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get active rules
     * GET /api/v1/rules/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<RuleResponse>> getActiveRules() {
        log.info("Fetching active rules");
        List<RuleResponse> rules = ruleService.getActiveRules();
        return ResponseEntity.ok(rules);
    }

    /**
     * Get rules by type
     * GET /api/v1/rules/type/{type}
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<RuleResponse>> getRulesByType(@PathVariable RuleType type) {
        log.info("Fetching rules of type: {}", type);
        List<RuleResponse> rules = ruleService.getRulesByType(type);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get rules by status
     * GET /api/v1/rules/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<RuleResponse>> getRulesByStatus(@PathVariable RuleStatus status) {
        log.info("Fetching rules with status: {}", status);
        List<RuleResponse> rules = ruleService.getRulesByStatus(status);
        return ResponseEntity.ok(rules);
    }

    /**
     * Search rules
     * GET /api/v1/rules/search?q={searchTerm}
     */
    @GetMapping("/search")
    public ResponseEntity<List<RuleResponse>> searchRules(@RequestParam String q) {
        log.info("Searching rules with term: {}", q);
        List<RuleResponse> rules = ruleService.searchRules(q);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get most triggered rules
     * GET /api/v1/rules/top-triggered
     */
    @GetMapping("/top-triggered")
    public ResponseEntity<List<RuleResponse>> getMostTriggeredRules() {
        log.info("Fetching most triggered rules");
        List<RuleResponse> rules = ruleService.getMostTriggeredRules();
        return ResponseEntity.ok(rules);
    }

    /**
     * Get rule statistics
     * GET /api/v1/rules/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<RuleStatistics> getRuleStatistics() {
        log.info("Fetching rule statistics");
        RuleStatistics stats = RuleStatistics.builder()
                .totalRules(ruleService.getTotalRulesCount())
                .activeRules(ruleService.getActiveRulesCount())
                .build();
        return ResponseEntity.ok(stats);
    }

    /**
     * Inner class for statistics response
     */
    @lombok.Data
    @lombok.Builder
    public static class RuleStatistics {
        private Long totalRules;
        private Long activeRules;
    }
}
