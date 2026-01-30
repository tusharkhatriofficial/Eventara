package com.eventara.rule.repository;

import com.eventara.rule.entity.AlertRule;
import com.eventara.rule.enums.RuleStatus;
import com.eventara.rule.enums.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RuleRepository extends JpaRepository<AlertRule, Long> {

    // Find by name
    Optional<AlertRule> findByName(String name);

    // Check if rule exists by name
    boolean existsByName(String name);

    // Find all active rules
    List<AlertRule> findByStatus(RuleStatus status);

    // Find active rules (commonly used for evaluation)
    @Query("SELECT r FROM AlertRule r WHERE r.status = 'ACTIVE'")
    List<AlertRule> findAllActiveRules();

    // Find rules by type
    List<AlertRule> findByRuleType(RuleType ruleType);

    // Find active rules by type
    List<AlertRule> findByStatusAndRuleType(RuleStatus status, RuleType ruleType);

    // Find rules by type and status (for RealTimeRuleEvaluator)
    List<AlertRule> findByRuleTypeAndStatus(RuleType ruleType, RuleStatus status);

    // Find rules by severity
    @Query("SELECT r FROM AlertRule r WHERE r.severity = :severity")
    List<AlertRule> findBySeverity(@Param("severity") String severity);

    // Find rules created by user
    List<AlertRule> findByCreatedBy(String createdBy);

    // Find rules created after a specific date
    List<AlertRule> findByCreatedAtAfter(LocalDateTime date);

    // Find rules that were triggered recently
    List<AlertRule> findByLastTriggeredAtAfter(LocalDateTime date);

    // Find rules with trigger count greater than
    @Query("SELECT r FROM AlertRule r WHERE r.triggerCount > :count")
    List<AlertRule> findByTriggerCountGreaterThan(@Param("count") Integer count);

    // Search rules by name or description
    @Query("SELECT r FROM AlertRule r WHERE " +
            "LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<AlertRule> searchRules(@Param("searchTerm") String searchTerm);

    // Count rules by status
    long countByStatus(RuleStatus status);

    // Count rules by type
    long countByRuleType(RuleType ruleType);

    // Get rules ordered by trigger count (most triggered first)
    @Query("SELECT r FROM AlertRule r ORDER BY r.triggerCount DESC")
    List<AlertRule> findMostTriggeredRules();

    // Get recently created rules
    @Query("SELECT r FROM AlertRule r ORDER BY r.createdAt DESC")
    List<AlertRule> findRecentlyCreatedRules();
}
