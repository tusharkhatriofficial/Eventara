package com.eventara.rule.service;

import com.eventara.rule.dto.request.CreateRuleRequest;
import com.eventara.rule.dto.request.UpdateRuleRequest;
import com.eventara.rule.dto.request.TestRuleRequest;
import com.eventara.rule.dto.response.RuleResponse;
import com.eventara.rule.dto.response.RuleTestResult;
import com.eventara.rule.entity.AlertRule;
import com.eventara.rule.enums.RuleStatus;
import com.eventara.rule.enums.RuleType;
import lombok.Builder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

public interface RuleService {

    // CRUD Operations
    RuleResponse createRule(CreateRuleRequest request);
    RuleResponse updateRule(Long id, UpdateRuleRequest request);
    void deleteRule(Long id);
    RuleResponse getRuleById(Long id);
    List<RuleResponse> getAllRules();

    // Status Management
    RuleResponse enableRule(Long id);
    RuleResponse disableRule(Long id);
    RuleResponse archiveRule(Long id);

    // Query Methods
    List<RuleResponse> getActiveRules();
    List<RuleResponse> getRulesByType(RuleType type);
    List<RuleResponse> getRulesByStatus(RuleStatus status);
    List<RuleResponse> searchRules(String searchTerm);

    // Rule Testing
    RuleTestResult testRule(TestRuleRequest request);
    RuleTestResult testRuleById(Long id);

    // Statistics
    long getTotalRulesCount();
    long getActiveRulesCount();
    List<RuleResponse> getMostTriggeredRules();

    // Internal methods (for Drools engine)
    AlertRule getAlertRuleEntity(Long id);
    List<AlertRule> getAllActiveRuleEntities();
    void updateLastTriggered(Long ruleId);
    void incrementTriggerCount(Long ruleId);
}
