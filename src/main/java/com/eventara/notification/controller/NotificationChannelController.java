package com.eventara.notification.controller;

import com.eventara.notification.dto.request.CreateNotificationChannelRequest;
import com.eventara.notification.dto.request.UpdateNotificationChannelRequest;
import com.eventara.notification.dto.response.NotificationChannelResponse;
import com.eventara.notification.dto.response.NotificationResult;
import com.eventara.notification.entity.NotificationChannel;
import com.eventara.notification.enums.ChannelType;
import com.eventara.notification.exception.NotificationChannelNotFoundException;
import com.eventara.notification.repository.NotificationChannelRepository;
import com.eventara.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for managing notification channels
 */
@RestController
@RequestMapping("/api/v1/notifications/channels")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Channels", description = "APIs for managing notification channels")
public class NotificationChannelController {

    private final NotificationChannelRepository channelRepository;
    private final NotificationService notificationService;

    @PostMapping
    @Operation(summary = "Create notification channel", description = "Create a new notification channel (webhook, email, slack, etc.)")
    public ResponseEntity<NotificationChannelResponse> createChannel(
            @RequestBody CreateNotificationChannelRequest request) {

        log.info("Creating notification channel: {} (type: {})", request.getName(), request.getChannelType());

        // Check if channel name already exists
        if (channelRepository.existsByName(request.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(null);
        }

        NotificationChannel channel = NotificationChannel.builder()
                .channelType(request.getChannelType())
                .name(request.getName())
                .description(request.getDescription())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .config(request.getConfig())
                .rateLimitPerMinute(request.getRateLimitPerMinute())
                .rateLimitPerHour(request.getRateLimitPerHour())
                .createdBy(request.getCreatedBy())
                .totalSent(0)
                .totalFailed(0)
                .build();

        NotificationChannel saved = channelRepository.save(channel);
        log.info("Created notification channel with ID: {}", saved.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToResponse(saved));
    }

    @GetMapping
    @Operation(summary = "List all notification channels")
    public ResponseEntity<List<NotificationChannelResponse>> getAllChannels(
            @RequestParam(required = false) ChannelType type,
            @RequestParam(required = false) Boolean enabled) {

        List<NotificationChannel> channels;

        if (type != null && enabled != null) {
            channels = channelRepository.findByChannelTypeAndEnabled(type, enabled);
        } else if (type != null) {
            channels = channelRepository.findByChannelType(type);
        } else if (enabled != null) {
            channels = channelRepository.findByEnabled(enabled);
        } else {
            channels = channelRepository.findAll();
        }

        List<NotificationChannelResponse> responses = channels.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification channel by ID")
    public ResponseEntity<NotificationChannelResponse> getChannelById(@PathVariable Long id) {
        NotificationChannel channel = channelRepository.findById(id)
                .orElseThrow(() -> new NotificationChannelNotFoundException("Channel not found: " + id));

        return ResponseEntity.ok(mapToResponse(channel));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update notification channel")
    public ResponseEntity<NotificationChannelResponse> updateChannel(
            @PathVariable Long id,
            @RequestBody UpdateNotificationChannelRequest request) {

        log.info("Updating notification channel: {}", id);

        NotificationChannel channel = channelRepository.findById(id)
                .orElseThrow(() -> new NotificationChannelNotFoundException("Channel not found: " + id));

        // Update fields
        if (request.getName() != null) {
            channel.setName(request.getName());
        }
        if (request.getDescription() != null) {
            channel.setDescription(request.getDescription());
        }
        if (request.getEnabled() != null) {
            channel.setEnabled(request.getEnabled());
        }
        if (request.getConfig() != null) {
            channel.setConfig(request.getConfig());
        }
        if (request.getRateLimitPerMinute() != null) {
            channel.setRateLimitPerMinute(request.getRateLimitPerMinute());
        }
        if (request.getRateLimitPerHour() != null) {
            channel.setRateLimitPerHour(request.getRateLimitPerHour());
        }

        NotificationChannel updated = channelRepository.save(channel);
        log.info("Updated notification channel: {}", id);

        return ResponseEntity.ok(mapToResponse(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification channel")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long id) {
        log.info("Deleting notification channel: {}", id);

        if (!channelRepository.existsById(id)) {
            throw new NotificationChannelNotFoundException("Channel not found: " + id);
        }

        channelRepository.deleteById(id);
        log.info("Deleted notification channel: {}", id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Test notification channel", description = "Send a test notification to verify channel configuration")
    public ResponseEntity<NotificationResult> testChannel(@PathVariable Long id) {
        log.info("Testing notification channel: {}", id);

        NotificationResult result = notificationService.testChannel(id);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get channel statistics")
    public ResponseEntity<ChannelStats> getChannelStats() {
        long totalChannels = channelRepository.count();
        long enabledChannels = channelRepository.countByEnabled(true);
        long webhookChannels = channelRepository.countByChannelType(ChannelType.WEBHOOK);
        long emailChannels = channelRepository.countByChannelType(ChannelType.EMAIL);
        long slackChannels = channelRepository.countByChannelType(ChannelType.SLACK);

        ChannelStats stats = new ChannelStats();
        stats.total = totalChannels;
        stats.enabled = enabledChannels;
        stats.webhooks = webhookChannels;
        stats.emails = emailChannels;
        stats.slack = slackChannels;

        return ResponseEntity.ok(stats);
    }

    /**
     * Stats response DTO
     */
    public static class ChannelStats {
        public long total;
        public long enabled;
        public long webhooks;
        public long emails;
        public long slack;
    }

    /**
     * Map entity to response DTO
     */
    private NotificationChannelResponse mapToResponse(NotificationChannel channel) {
        return NotificationChannelResponse.builder()
                .id(channel.getId())
                .channelType(channel.getChannelType())
                .name(channel.getName())
                .description(channel.getDescription())
                .enabled(channel.getEnabled())
                .config(channel.getConfig())
                .rateLimitPerMinute(channel.getRateLimitPerMinute())
                .rateLimitPerHour(channel.getRateLimitPerHour())
                .lastUsedAt(channel.getLastUsedAt())
                .totalSent(channel.getTotalSent())
                .totalFailed(channel.getTotalFailed())
                .createdAt(channel.getCreatedAt())
                .updatedAt(channel.getUpdatedAt())
                .createdBy(channel.getCreatedBy())
                .build();
    }
}
