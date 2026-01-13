package com.eventara.analytics.service;

import com.eventara.drools.fact.MetricsFact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsAggregationService {

    private final MetricsCalculator metricsCalculator;

    // Metrics calculation is now handled per rule group in RuleExecutionService
}