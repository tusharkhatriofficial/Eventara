package com.eventara.analytics.controller;

import com.eventara.analytics.service.ComprehensiveMetricsService;
import com.eventara.common.dto.ComprehensiveMetricsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

@Controller
@EnableScheduling
public class MetricsWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(MetricsWebSocketController.class);

    @Autowired
    private ComprehensiveMetricsService metricsService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Push metrics to all connected clients every second
     */
    @Scheduled(fixedRate = 1000) // Every 1 second
    public void pushMetrics() {
        try {
            ComprehensiveMetricsDto metrics = metricsService.getComprehensiveMetrics();

            // Push to all subscribers of /topic/metrics
            messagingTemplate.convertAndSend("/topic/metrics", metrics);

            logger.debug("Pushed metrics to WebSocket clients: {} total events",
                    metrics.getSummary().getTotalEvents());

        } catch (Exception e) {
            logger.error("Error pushing metrics via WebSocket", e);
        }
    }

    /**
     * Handle client subscription requests
     */
    @MessageMapping("/subscribe")
    @SendTo("/topic/metrics")
    public ComprehensiveMetricsDto subscribeToMetrics() {
        logger.info("Client subscribed to metrics");
        return metricsService.getComprehensiveMetrics();
    }
}
