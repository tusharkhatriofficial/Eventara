package com.eventara.rule.repository;

import com.eventara.rule.entity.RuleExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RuleExecutionLogRepository extends JpaRepository<RuleExecutionLog, Long> {

    // Find execution logs by rule ID
    List<RuleExecutionLog> findByRuleId(Long ruleId);

    // Find execution logs by rule ID with pagination
    Page<RuleExecutionLog> findByRuleId(Long ruleId, Pageable pageable);

    // Find logs where condition was met
    List<RuleExecutionLog> findByConditionMet(Boolean conditionMet);

    // Find logs for a specific rule where condition was met
    List<RuleExecutionLog> findByRuleIdAndConditionMet(Long ruleId, Boolean conditionMet);

    // Find recent execution logs
    List<RuleExecutionLog> findByExecutedAtAfter(LocalDateTime date);

    // Find execution logs for a rule in a time range
    @Query("SELECT l FROM RuleExecutionLog l WHERE l.ruleId = :ruleId " +
            "AND l.executedAt BETWEEN :startDate AND :endDate " +
            "ORDER BY l.executedAt DESC")
    List<RuleExecutionLog> findByRuleIdAndDateRange(
            @Param("ruleId") Long ruleId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Find failed executions (with errors)
    @Query("SELECT l FROM RuleExecutionLog l WHERE l.errorMessage IS NOT NULL")
    List<RuleExecutionLog> findFailedExecutions();

    // Get average execution time for a rule
    @Query("SELECT AVG(l.executionTimeMs) FROM RuleExecutionLog l WHERE l.ruleId = :ruleId")
    Double getAverageExecutionTime(@Param("ruleId") Long ruleId);

    // Get execution count for a rule
    long countByRuleId(Long ruleId);

    // Get count of successful rule triggers
    @Query("SELECT COUNT(l) FROM RuleExecutionLog l WHERE l.ruleId = :ruleId AND l.conditionMet = true")
    long countSuccessfulTriggers(@Param("ruleId") Long ruleId);

    // Delete old logs (for cleanup)
    void deleteByExecutedAtBefore(LocalDateTime date);

    // Get recent executions for all rules
    @Query("SELECT l FROM RuleExecutionLog l ORDER BY l.executedAt DESC")
    List<RuleExecutionLog> findRecentExecutions(Pageable pageable);
}
