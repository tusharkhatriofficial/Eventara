package com.eventara.notification.controller;

import com.eventara.notification.entity.NotificationLog;
import com.eventara.notification.enums.ChannelType;
import com.eventara.notification.enums.NotificationStatus;
import com.eventara.notification.repository.NotificationLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller for viewing notification logs
 */
@RestController
@RequestMapping("/api/v1/notifications/logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Logs", description = "APIs for viewing notification delivery history")
public class NotificationLogController {

    private final NotificationLogRepository logRepository;

    @GetMapping
    @Operation(summary = "List notification logs", description = "Get notification logs with optional filtering")
    public ResponseEntity<Page<NotificationLog>> getNotificationLogs(
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) Long alertId,
            @RequestParam(required = false) ChannelType channelType,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Apply filters
        if (channelId != null) {
            return ResponseEntity.ok(Page.empty(pageable));
        }

        if (status != null) {
            return ResponseEntity.ok(logRepository.findByStatus(status, pageable));
        }

        if (alertId != null) {
            List<NotificationLog> logs = logRepository.findByAlertId(alertId);
            return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(logs, pageable, logs.size()));
        }

        if (channelType != null) {
            List<NotificationLog> logs = logRepository.findByChannelType(channelType);
            return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(logs, pageable, logs.size()));
        }

        Page<NotificationLog> logs = logRepository.findAll(pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification log by ID")
    public ResponseEntity<NotificationLog> getNotificationLogById(@PathVariable Long id) {
        NotificationLog log = logRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification log not found: " + id));

        return ResponseEntity.ok(log);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get notification statistics")
    public ResponseEntity<NotificationStats> getNotificationStats(
            @RequestParam(defaultValue = "24") int hours) {

        // Note: Could filter by time range in the future using hours parameter
        // LocalDateTime since = LocalDateTime.now().minusHours(hours);

        long totalSent = logRepository.countByStatus(NotificationStatus.SENT);
        long totalFailed = logRepository.countByStatus(NotificationStatus.FAILED);
        long total = totalSent + totalFailed;

        double successRate = total > 0 ? (totalSent * 100.0 / total) : 0;

        // Note: statsByChannel and avgDeliveryTime could be added to stats in the
        // future
        // List<Object[]> statsByChannel =
        // logRepository.getNotificationStatsByChannel(since);
        // List<Object[]> avgDeliveryTime =
        // logRepository.getAverageDeliveryTimeByChannel(since);

        NotificationStats stats = new NotificationStats();
        stats.totalSent = totalSent;
        stats.totalFailed = totalFailed;
        stats.total = total;
        stats.successRate = Math.round(successRate * 100.0) / 100.0;
        stats.periodHours = hours;

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/failed")
    @Operation(summary = "Get failed notifications")
    public ResponseEntity<List<NotificationLog>> getFailedNotifications(
            @RequestParam(defaultValue = "100") int limit) {

        List<NotificationLog> failedLogs = logRepository.findFailedNotifications();

        // Limit results
        if (failedLogs.size() > limit) {
            failedLogs = failedLogs.subList(0, limit);
        }

        return ResponseEntity.ok(failedLogs);
    }

    @GetMapping("/alert/{alertId}")
    @Operation(summary = "Get notifications for a specific alert")
    public ResponseEntity<List<NotificationLog>> getNotificationsByAlert(@PathVariable Long alertId) {
        List<NotificationLog> logs = logRepository.findByAlertId(alertId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/channel/{channelId}")
    @Operation(summary = "Get notifications for a specific channel")
    public ResponseEntity<List<NotificationLog>> getNotificationsByChannel(@PathVariable Long channelId) {
        List<NotificationLog> logs = logRepository.findByChannelId(channelId);
        return ResponseEntity.ok(logs);
    }

    /**
     * Statistics DTO
     */
    public static class NotificationStats {
        public long total;
        public long totalSent;
        public long totalFailed;
        public double successRate;
        public int periodHours;
    }
}
