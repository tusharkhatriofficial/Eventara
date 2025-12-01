package com.eventara.notification.repository;

import com.eventara.notification.entity.NotificationChannel;
import com.eventara.notification.enums.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, Long> {

    // Find channel by name
    Optional<NotificationChannel> findByName(String name);

    // Check if channel exists by name
    boolean existsByName(String name);

    // Find all enabled channels
    List<NotificationChannel> findByEnabled(Boolean enabled);

    // Find channels by type
    List<NotificationChannel> findByChannelType(ChannelType channelType);

    // Find enabled channels by type
    List<NotificationChannel> findByChannelTypeAndEnabled(ChannelType channelType, Boolean enabled);

    // Get all enabled channels (for notification dispatch)
    @Query("SELECT c FROM NotificationChannel c WHERE c.enabled = true")
    List<NotificationChannel> findAllEnabled();

    // Find channels created by user
    List<NotificationChannel> findByCreatedBy(String createdBy);

    // Get channel usage statistics
    @Query("SELECT c FROM NotificationChannel c ORDER BY c.totalSent DESC")
    List<NotificationChannel> findMostUsedChannels();

    // Find channels with high failure rate
    @Query("SELECT c FROM NotificationChannel c WHERE " +
            "c.totalFailed > 0 AND " +
            "(CAST(c.totalFailed AS float) / CAST(c.totalSent AS float)) > :threshold")
    List<NotificationChannel> findChannelsWithHighFailureRate(@Param("threshold") Double threshold);

    // Count channels by type
    long countByChannelType(ChannelType channelType);

    // Count enabled channels
    long countByEnabled(Boolean enabled);
}
