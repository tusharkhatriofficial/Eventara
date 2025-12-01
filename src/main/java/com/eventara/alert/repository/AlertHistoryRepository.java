package com.eventara.alert.repository;

import com.eventara.alert.entity.AlertHistory;
import com.eventara.alert.enums.AlertSeverity;
import com.eventara.alert.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    // Find alerts by rule ID
    List<AlertHistory> findByRuleId(Long ruleId);

    // Find alerts by rule ID with pagination
    Page<AlertHistory> findByRuleId(Long ruleId, Pageable pageable);

    // Find alerts by status
    List<AlertHistory> findByStatus(AlertStatus status);

    // Find active alerts
    @Query("SELECT a FROM AlertHistory a WHERE a.status = 'ACTIVE' ORDER BY a.triggeredAt DESC")
    List<AlertHistory> findActiveAlerts();

    // Find alerts by severity
    List<AlertHistory> findBySeverity(AlertSeverity severity);

    // Find critical active alerts
    @Query("SELECT a FROM AlertHistory a WHERE a.status = 'ACTIVE' AND a.severity = 'CRITICAL' " +
            "ORDER BY a.triggeredAt DESC")
    List<AlertHistory> findCriticalActiveAlerts();

    // Find alerts triggered in time range
    List<AlertHistory> findByTriggeredAtBetween(LocalDateTime start, LocalDateTime end);

    // Find alerts by status and time range
    @Query("SELECT a FROM AlertHistory a WHERE a.status = :status " +
            "AND a.triggeredAt BETWEEN :startDate AND :endDate " +
            "ORDER BY a.triggeredAt DESC")
    Page<AlertHistory> findByStatusAndDateRange(
            @Param("status") AlertStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // Find recent alerts for a specific rule
    @Query("SELECT a FROM AlertHistory a WHERE a.ruleId = :ruleId " +
            "AND a.triggeredAt > :since ORDER BY a.triggeredAt DESC")
    List<AlertHistory> findRecentAlertsByRule(
            @Param("ruleId") Long ruleId,
            @Param("since") LocalDateTime since
    );

    // Check if rule has active alerts (for suppression)
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM AlertHistory a WHERE a.ruleId = :ruleId " +
            "AND a.status = 'ACTIVE' AND a.triggeredAt > :since")
    boolean hasActiveAlertsInTimeWindow(
            @Param("ruleId") Long ruleId,
            @Param("since") LocalDateTime since
    );

    // Count alerts by rule
    long countByRuleId(Long ruleId);

    // Count alerts by status
    long countByStatus(AlertStatus status);

    // Count alerts by severity
    long countBySeverity(AlertSeverity severity);

    // Count alerts in time range
    @Query("SELECT COUNT(a) FROM AlertHistory a WHERE a.triggeredAt BETWEEN :start AND :end")
    long countAlertsInTimeRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Get alert statistics by severity
    @Query("SELECT a.severity, COUNT(a) FROM AlertHistory a " +
            "WHERE a.triggeredAt > :since GROUP BY a.severity")
    List<Object[]> getAlertCountBySeverity(@Param("since") LocalDateTime since);

    // Get alert statistics by rule
    @Query("SELECT a.ruleName, COUNT(a) FROM AlertHistory a " +
            "WHERE a.triggeredAt > :since GROUP BY a.ruleName ORDER BY COUNT(a) DESC")
    List<Object[]> getAlertCountByRule(@Param("since") LocalDateTime since);

    // Find unacknowledged alerts
    @Query("SELECT a FROM AlertHistory a WHERE a.status = 'ACTIVE' " +
            "AND a.acknowledgedAt IS NULL ORDER BY a.severity DESC, a.triggeredAt DESC")
    List<AlertHistory> findUnacknowledgedAlerts();

    // Find alerts acknowledged by user
    List<AlertHistory> findByAcknowledgedBy(String username);

    // Find alerts resolved by user
    List<AlertHistory> findByResolvedBy(String username);

    // Delete old resolved alerts (for cleanup)
    void deleteByStatusAndResolvedAtBefore(AlertStatus status, LocalDateTime date);

    // Search alerts by message or description
    @Query("SELECT a FROM AlertHistory a WHERE " +
            "LOWER(a.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(a.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<AlertHistory> searchAlerts(@Param("searchTerm") String searchTerm);
}
