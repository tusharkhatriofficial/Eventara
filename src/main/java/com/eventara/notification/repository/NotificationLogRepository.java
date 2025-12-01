package com.eventara.notification.repository;

import com.eventara.notification.entity.NotificationLog;
import com.eventara.notification.enums.ChannelType;
import com.eventara.notification.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    // Find logs by alert ID
    List<NotificationLog> findByAlertId(Long alertId);

    // Find logs by channel ID
    List<NotificationLog> findByChannelId(Long channelId);

    // Find logs by status
    List<NotificationLog> findByStatus(NotificationStatus status);

    // Find pending/retrying notifications (for retry processing)
    @Query("SELECT n FROM NotificationLog n WHERE n.status IN ('PENDING', 'RETRYING') " +
            "AND (n.nextRetryAt IS NULL OR n.nextRetryAt <= :now)")
    List<NotificationLog> findNotificationsToRetry(@Param("now") LocalDateTime now);

    // Find failed notifications
    @Query("SELECT n FROM NotificationLog n WHERE n.status = 'FAILED' ORDER BY n.createdAt DESC")
    List<NotificationLog> findFailedNotifications();

    // Find notifications by channel type
    List<NotificationLog> findByChannelType(ChannelType channelType);

    // Find notifications in time range
    List<NotificationLog> findBySentAtBetween(LocalDateTime start, LocalDateTime end);

    // Find notifications with pagination
    Page<NotificationLog> findByStatus(NotificationStatus status, Pageable pageable);

    // Count notifications by status
    long countByStatus(NotificationStatus status);

    // Count notifications by channel type
    long countByChannelType(ChannelType channelType);

    // Get notification statistics by channel
    @Query("SELECT n.channelType, n.status, COUNT(n) FROM NotificationLog n " +
            "WHERE n.sentAt > :since GROUP BY n.channelType, n.status")
    List<Object[]> getNotificationStatsByChannel(@Param("since") LocalDateTime since);

    // Get average delivery time by channel type
    @Query("SELECT n.channelType, AVG(n.deliveryTimeMs) FROM NotificationLog n " +
            "WHERE n.status = 'SENT' AND n.sentAt > :since GROUP BY n.channelType")
    List<Object[]> getAverageDeliveryTimeByChannel(@Param("since") LocalDateTime since);

    // Delete old notification logs (for cleanup)
    void deleteByCreatedAtBefore(LocalDateTime date);

    // Find notifications that need retry
    @Query("SELECT n FROM NotificationLog n WHERE n.status = 'RETRYING' " +
            "AND n.retryCount < :maxRetries AND n.nextRetryAt <= :now")
    List<NotificationLog> findNotificationsNeedingRetry(
            @Param("maxRetries") Integer maxRetries,
            @Param("now") LocalDateTime now
    );
}
