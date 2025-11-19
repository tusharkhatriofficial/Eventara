package com.eventara.analytics.controller;

import com.eventara.analytics.service.ComprehensiveMetricsService;
import com.eventara.common.dto.ComprehensiveMetricsDto;
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

    @GetMapping
    @Operation(summary = "Get comprehensive metrics")
    public ResponseEntity<ComprehensiveMetricsDto> getMetrics() {
        return ResponseEntity.ok(metricsService.getComprehensiveMetrics());
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset all metrics")
    public ResponseEntity<String> resetMetrics() {
        metricsService.resetMetrics();
        return ResponseEntity.ok("Metrics reset successfully");
    }
}
