package com.eventara.analytics.service;

import com.eventara.drools.fact.MetricsFact;
import com.eventara.drools.service.RuleExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsAggregationService {

    private final RuleExecutionService ruleExecutionService;
    private final MetricsCalculator metricsCalculator;

    /**
     * Calculate and execute rules every minute
     */
    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    public void aggregateAndExecuteRules() {
        log.debug("Starting metrics aggregation");

        try {
            // 1. Calculate current metrics from database/cache
            MetricsFact metrics = metricsCalculator.calculateCurrentMetrics();

            // 2. Log key metrics for debugging
            log.info("Metrics Aggregated - Error Rate: {}%, Events/sec: {}, Active Users: {}",
                    String.format("%.2f", metrics.getErrorRate()),
                    String.format("%.2f", metrics.getEventsPerSecond()),
                    metrics.getActiveUsersLast1Hour());

            // 3. Fire rules against these metrics
            ruleExecutionService.executeRules(metrics);

        } catch (Exception e) {
            log.error("Error during metrics aggregation and rule execution", e);
        }
    }
}