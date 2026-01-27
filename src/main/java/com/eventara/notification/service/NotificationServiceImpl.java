package com.eventara.notification.service;

import com.eventara.notification.dto.NotificationMessage;
import com.eventara.notification.dto.response.NotificationResult;
import com.eventara.notification.entity.NotificationChannel;
import com.eventara.notification.entity.NotificationLog;
import com.eventara.notification.enums.ChannelType;
import com.eventara.notification.enums.NotificationStatus;
import com.eventara.notification.exception.NotificationChannelNotFoundException;
import com.eventara.notification.repository.NotificationChannelRepository;
import com.eventara.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of NotificationService
 * Orchestrates notification delivery across different channels
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationChannelRepository channelRepository;
    private final NotificationLogRepository logRepository;
    private final WebhookNotificationHandler webhookHandler;

    @Override
    @Async("notificationExecutor")
    public CompletableFuture<List<NotificationResult>> sendNotification(
            NotificationMessage message,
            List<String> channelNames) {

        log.info("Sending notification for alert {} to {} channels",
                message.getAlertId(), channelNames.size());

        List<NotificationResult> results = new ArrayList<>();

        for (String channelName : channelNames) {
            try {
                NotificationChannel channel = channelRepository.findByName(channelName)
                        .orElseThrow(() -> new NotificationChannelNotFoundException(
                                "Channel not found: " + channelName));

                // Check if channel is enabled
                if (!channel.getEnabled()) {
                    log.warn("Channel {} is disabled, skipping", channelName);
                    NotificationResult result = NotificationResult.builder()
                            .channelId(channel.getId())
                            .channelName(channel.getName())
                            .status(NotificationStatus.FAILED)
                            .errorMessage("Channel is disabled")
                            .build();
                    results.add(result);
                    logNotification(message, channel, result);
                    continue;
                }

                // Apply rate limiting
                if (!checkRateLimit(channel)) {
                    log.warn("Rate limit exceeded for channel {}", channelName);
                    NotificationResult result = NotificationResult.builder()
                            .channelId(channel.getId())
                            .channelName(channel.getName())
                            .status(NotificationStatus.FAILED)
                            .errorMessage("Rate limit exceeded")
                            .build();
                    results.add(result);
                    logNotification(message, channel, result);
                    continue;
                }

                // Send notification
                NotificationResult result = sendToChannel(message, channel);
                results.add(result);

                // Log the notification
                logNotification(message, channel, result);

                // Update channel statistics
                updateChannelStats(channel, result);

            } catch (Exception e) {
                log.error("Error sending notification to channel {}: {}",
                        channelName, e.getMessage(), e);
                NotificationResult result = NotificationResult.builder()
                        .channelName(channelName)
                        .status(NotificationStatus.FAILED)
                        .errorMessage(e.getMessage())
                        .build();
                results.add(result);
            }
        }

        log.info("Sent {} notifications for alert {}", results.size(), message.getAlertId());
        return CompletableFuture.completedFuture(results);
    }

    @Override
    public NotificationResult sendToChannel(NotificationMessage message, NotificationChannel channel) {
        log.debug("Sending to channel {} (type: {})", channel.getName(), channel.getChannelType());

        switch (channel.getChannelType()) {
            case WEBHOOK:
                return webhookHandler.sendWebhook(message, channel);

            case EMAIL:
                // TODO: Implement email handler
                return buildNotImplementedResult(channel, "Email notifications not yet implemented");

            case SLACK:
                // TODO: Implement Slack handler
                return buildNotImplementedResult(channel, "Slack notifications not yet implemented");

            case SMS:
                // TODO: Implement SMS handler
                return buildNotImplementedResult(channel, "SMS notifications not yet implemented");

            case PAGERDUTY:
                // TODO: Implement PagerDuty handler
                return buildNotImplementedResult(channel, "PagerDuty notifications not yet implemented");

            default:
                return buildNotImplementedResult(channel, "Unknown channel type");
        }
    }

    @Override
    public NotificationResult testChannel(Long channelId) {
        NotificationChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new NotificationChannelNotFoundException(
                        "Channel not found with id: " + channelId));

        // Build test message
        NotificationMessage testMessage = NotificationMessage.builder()
                .alertId(0L)
                .ruleName("Test Alert")
                .severity(com.eventara.alert.enums.AlertSeverity.INFO)
                .subject("Test Notification")
                .message("This is a test notification from Eventara")
                .triggeredAt(LocalDateTime.now())
                .build();

        return sendToChannel(testMessage, channel);
    }

    /**
     * Check if channel has exceeded rate limits
     */
    private boolean checkRateLimit(NotificationChannel channel) {
        if (channel.getRateLimitPerMinute() != null && channel.getRateLimitPerMinute() > 0) {
            LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
            long sentInLastMinute = logRepository.countByChannelIdAndCreatedAtAfterAndStatus(
                    channel.getId(), oneMinuteAgo, NotificationStatus.SENT);

            if (sentInLastMinute >= channel.getRateLimitPerMinute()) {
                return false;
            }
        }

        if (channel.getRateLimitPerHour() != null && channel.getRateLimitPerHour() > 0) {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long sentInLastHour = logRepository.countByChannelIdAndCreatedAtAfterAndStatus(
                    channel.getId(), oneHourAgo, NotificationStatus.SENT);

            if (sentInLastHour >= channel.getRateLimitPerHour()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Log notification attempt to database
     */
    @Transactional
    protected void logNotification(NotificationMessage message, NotificationChannel channel,
            NotificationResult result) {
        NotificationLog log = NotificationLog.builder()
                .alertId(message.getAlertId())
                .channelId(channel.getId())
                .channelType(channel.getChannelType())
                .recipient(extractRecipient(channel))
                .status(result.getStatus())
                .sentAt(result.getSentAt())
                .subject(message.getSubject())
                .message(message.getMessage())
                .responseCode(result.getResponseCode())
                .responseBody(result.getResponseBody())
                .errorMessage(result.getErrorMessage())
                .retryCount(result.getRetryCount())
                .deliveryTimeMs(result.getDeliveryTimeMs())
                .build();

        logRepository.save(log);
    }

    /**
     * Update channel statistics
     */
    @Transactional
    protected void updateChannelStats(NotificationChannel channel, NotificationResult result) {
        channel.setLastUsedAt(LocalDateTime.now());

        if (result.getStatus() == NotificationStatus.SENT) {
            channel.setTotalSent(
                    (channel.getTotalSent() != null ? channel.getTotalSent() : 0) + 1);
        } else {
            channel.setTotalFailed(
                    (channel.getTotalFailed() != null ? channel.getTotalFailed() : 0) + 1);
        }

        channelRepository.save(channel);
    }

    /**
     * Extract recipient information from channel config
     */
    private String extractRecipient(NotificationChannel channel) {
        if (channel.getChannelType() == ChannelType.WEBHOOK) {
            return channel.getConfig() != null && channel.getConfig().containsKey("url")
                    ? channel.getConfig().get("url").toString()
                    : "unknown";
        }
        // For other channel types, extract email, phone, etc.
        return "N/A";
    }

    /**
     * Build result for not implemented channel types
     */
    private NotificationResult buildNotImplementedResult(NotificationChannel channel, String message) {
        return NotificationResult.builder()
                .channelId(channel.getId())
                .channelName(channel.getName())
                .status(NotificationStatus.FAILED)
                .errorMessage(message)
                .sentAt(LocalDateTime.now())
                .build();
    }
}
