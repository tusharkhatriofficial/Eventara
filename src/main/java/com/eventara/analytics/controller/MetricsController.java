package com.eventara.analytics.controller;

import com.eventara.analytics.service.ComprehensiveMetricsService;
import com.eventara.common.dto.ComprehensiveMetricsDto;
import com.eventara.metrics.config.MetricsProperties;
import com.eventara.metrics.service.DistributedMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "Analytics", description = "Comprehensive real-time event analytics")
public class MetricsController {

    @Autowired
    private ComprehensiveMetricsService metricsService;

    @Autowired
    private DistributedMetricsService distributedMetricsService;

    @Autowired
    private MetricsProperties metricsProperties;

    @GetMapping
    @Operation(summary = "Get comprehensive metrics")
    public ResponseEntity<ComprehensiveMetricsDto> getMetrics() {
        ComprehensiveMetricsDto metrics = metricsProperties.getDistributed().isEnabled()
                ? distributedMetricsService.getComprehensiveMetrics()
                : metricsService.getComprehensiveMetrics();
        return ResponseEntity.ok(metrics);
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset all metrics")
    public ResponseEntity<String> resetMetrics() {
        if (metricsProperties.getDistributed().isEnabled()) {
            distributedMetricsService.resetMetrics();
        } else {
            metricsService.resetMetrics();
        }
        return ResponseEntity.ok("Metrics reset successfully");
    }
}
