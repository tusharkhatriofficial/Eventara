package com.eventara.notification.service;

import com.eventara.notification.dto.NotificationMessage;
import com.eventara.notification.dto.response.NotificationResult;
import com.eventara.notification.entity.NotificationChannel;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for sending notifications across different channels
 */
public interface NotificationService {

    /**
     * Send notification to specified channels asynchronously
     *
     * @param message      The notification message
     * @param channelNames List of channel names to send to
     * @return Future with list of notification results
     */
    CompletableFuture<List<NotificationResult>> sendNotification(
            NotificationMessage message,
            List<String> channelNames);

    /**
     * Send notification to a single channel
     *
     * @param message The notification message
     * @param channel The notification channel
     * @return Notification result
     */
    NotificationResult sendToChannel(NotificationMessage message, NotificationChannel channel);

    /**
     * Test a notification channel
     *
     * @param channelId The channel ID to test
     * @return Test result
     */
    NotificationResult testChannel(Long channelId);
}
