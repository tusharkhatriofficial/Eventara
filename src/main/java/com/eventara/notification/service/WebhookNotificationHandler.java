package com.eventara.notification.service;

import com.eventara.notification.dto.NotificationMessage;
import com.eventara.notification.dto.response.NotificationResult;
import com.eventara.notification.entity.NotificationChannel;
import com.eventara.notification.enums.NotificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for sending webhook notifications
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookNotificationHandler {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send webhook notification with retry logic
     */
    public NotificationResult sendWebhook(NotificationMessage message, NotificationChannel channel) {
        String webhookUrl = extractWebhookUrl(channel);

        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return buildErrorResult(channel.getId(), "Webhook URL not configured");
        }

        int maxRetries = 3;
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
            try {
                long startTime = System.currentTimeMillis();

                // Build webhook payload
                Map<String, Object> payload = buildWebhookPayload(message);

                // Build HTTP headers
                HttpHeaders headers = buildHeaders(channel);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

                // Send POST request
                ResponseEntity<String> response = restTemplate.exchange(
                        webhookUrl,
                        HttpMethod.POST,
                        request,
                        String.class);

                long deliveryTime = System.currentTimeMillis() - startTime;

                log.info("Webhook sent successfully to {} for alert {}", webhookUrl, message.getAlertId());

                return NotificationResult.builder()
                        .channelId(channel.getId())
                        .channelName(channel.getName())
                        .status(NotificationStatus.SENT)
                        .sentAt(LocalDateTime.now())
                        .responseCode(response.getStatusCode().value())
                        .responseBody(response.getBody())
                        .retryCount(retryCount)
                        .deliveryTimeMs((int) deliveryTime)
                        .build();

            } catch (HttpStatusCodeException e) {
                lastException = e;
                log.warn("Webhook failed (attempt {}/{}) to {}: {} - {}",
                        retryCount + 1, maxRetries, webhookUrl, e.getStatusCode(), e.getResponseBodyAsString());

                // Don't retry for client errors (4xx)
                if (e.getStatusCode().is4xxClientError()) {
                    return buildErrorResult(channel.getId(), channel.getName(),
                            e.getStatusCode().value(), e.getResponseBodyAsString(),
                            "Client error: " + e.getMessage(), retryCount, 0);
                }

            } catch (Exception e) {
                lastException = e;
                log.warn("Webhook failed (attempt {}/{}) to {}: {}",
                        retryCount + 1, maxRetries, webhookUrl, e.getMessage());
            }

            retryCount++;

            if (retryCount < maxRetries) {
                try {
                    // Exponential backoff: 1s, 2s, 4s
                    Thread.sleep((long) Math.pow(2, retryCount - 1) * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // All retries failed
        return buildErrorResult(channel.getId(), channel.getName(), null, null,
                "Failed after " + maxRetries + " retries: " +
                        (lastException != null ? lastException.getMessage() : "Unknown error"),
                retryCount - 1, 0);
    }

    /**
     * Extract webhook URL from channel config
     */
    private String extractWebhookUrl(NotificationChannel channel) {
        Map<String, Object> config = channel.getConfig();
        if (config != null && config.containsKey("url")) {
            return config.get("url").toString();
        }
        return null;
    }

    /**
     * Build HTTP headers from channel config
     */
    private HttpHeaders buildHeaders(NotificationChannel channel) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> config = channel.getConfig();
        if (config != null && config.containsKey("headers")) {
            @SuppressWarnings("unchecked")
            Map<String, String> customHeaders = (Map<String, String>) config.get("headers");
            customHeaders.forEach(headers::set);
        }

        // Add authentication if configured
        if (config != null && config.containsKey("authToken")) {
            String token = config.get("authToken").toString();
            headers.setBearerAuth(token);
        }

        return headers;
    }

    /**
     * Build webhook payload from notification message
     */
    private Map<String, Object> buildWebhookPayload(NotificationMessage message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("alertId", message.getAlertId());
        payload.put("ruleName", message.getRuleName());
        payload.put("severity", message.getSeverity() != null ? message.getSeverity().toString() : "INFO");
        payload.put("subject", message.getSubject() != null ? message.getSubject() : "Alert Triggered");
        payload.put("message", message.getMessage());
        payload.put("thresholdValue", message.getThresholdValue());
        payload.put("actualValue", message.getActualValue());
        payload.put("triggeredAt", message.getTriggeredAt() != null ? message.getTriggeredAt().toString() : null);

        if (message.getContext() != null) {
            payload.put("context", message.getContext());
        }

        return payload;
    }

    /**
     * Build error result
     */
    private NotificationResult buildErrorResult(Long channelId, String errorMessage) {
        return buildErrorResult(channelId, null, null, null, errorMessage, 0, 0);
    }

    /**
     * Build error result with all details
     */
    private NotificationResult buildErrorResult(Long channelId, String channelName,
            Integer responseCode, String responseBody, String errorMessage,
            int retryCount, int deliveryTime) {
        return NotificationResult.builder()
                .channelId(channelId)
                .channelName(channelName)
                .status(NotificationStatus.FAILED)
                .sentAt(LocalDateTime.now())
                .responseCode(responseCode)
                .responseBody(responseBody)
                .errorMessage(errorMessage)
                .retryCount(retryCount)
                .deliveryTimeMs(deliveryTime)
                .build();
    }
}
