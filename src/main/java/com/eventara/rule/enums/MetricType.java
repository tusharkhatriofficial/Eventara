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
    TOTAL_UNIQUE_USERS
}
