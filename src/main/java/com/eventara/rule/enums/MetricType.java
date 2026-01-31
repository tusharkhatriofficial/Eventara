package com.eventara.rule.enums;

public enum MetricType {
    // Error Metrics
    ERROR_RATE,
    TOTAL_ERRORS,

    // Performance Metrics
    AVG_LATENCY,
    P50_LATENCY,
    P95_LATENCY,
    P99_LATENCY,
    MAX_LATENCY,
    MIN_LATENCY,

    // Throughput Metrics
    EVENTS_PER_SECOND,
    EVENTS_PER_MINUTE,
    EVENTS_PER_HOUR,
    EVENTS_PER_DAY,
    PEAK_THROUGHPUT,
    AVG_THROUGHPUT_1H,
    AVG_THROUGHPUT_24H,

    // Time Window Metrics
    EVENTS_LAST_1_MINUTE,
    EVENTS_LAST_5_MINUTES,
    EVENTS_LAST_15_MINUTES,
    EVENTS_LAST_1_HOUR,
    EVENTS_LAST_24_HOURS,

    // Summary Metrics
    TOTAL_EVENTS,
    UNIQUE_SOURCES,
    UNIQUE_EVENT_TYPES,
    UNIQUE_USERS,
    SYSTEM_HEALTH,

    // User Metrics
    ACTIVE_USERS_LAST_1_HOUR,
    ACTIVE_USERS_LAST_24_HOURS,
    TOTAL_UNIQUE_USERS,

    // Ratio/Derived Metrics (Phase 2)
    EVENT_RATIO, // Compare counts of two event types: numerator/denominator
    SOURCE_ERROR_RATE, // Error rate for a specific source
    EVENT_TYPE_COUNT, // Count of a specific event type

    // Rate of Change Metrics (Phase 3)
    ERROR_RATE_CHANGE, // % change in error rate vs previous window
    LATENCY_CHANGE, // % change in avg latency vs previous window
    THROUGHPUT_CHANGE, // % change in events/min vs previous window
    SPIKE_DETECTION, // Sudden increase detection

    // Baseline Comparison Metrics (Phase 5)
    ERROR_RATE_VS_BASELINE, // Current error rate vs historical baseline
    LATENCY_VS_BASELINE, // Current latency vs historical baseline
    THROUGHPUT_VS_BASELINE // Current throughput vs historical baseline
}
