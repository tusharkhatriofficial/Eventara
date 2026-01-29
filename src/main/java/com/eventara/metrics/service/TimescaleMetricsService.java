package com.eventara.metrics.service;

import com.eventara.metrics.model.MetricsBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * TimescaleDB-based metrics storage for historical data.
 * Uses hypertables with automatic time-partitioning and compression.
 */
@Service
public class TimescaleMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(TimescaleMetricsService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Insert metric buckets from Redis rollup.
     */
    public void insertBuckets(List<MetricsBucket> buckets) {
        if (buckets == null || buckets.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO metrics_buckets (
                    bucket_start, bucket_end, total_events, total_errors,
                    latency_sum, latency_count, latency_p50, latency_p95, latency_p99,
                    latency_min, latency_max, by_source, by_event_type, by_severity
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb)
                ON CONFLICT DO NOTHING
                """;

        for (MetricsBucket bucket : buckets) {
            try {
                jdbcTemplate.update(sql,
                        Timestamp.from(bucket.getBucketStart()),
                        Timestamp.from(bucket.getBucketEnd()),
                        bucket.getTotalEvents(),
                        bucket.getTotalErrors(),
                        bucket.getLatencySum(),
                        bucket.getLatencyCount(),
                        bucket.getLatencyP50(),
                        bucket.getLatencyP95(),
                        bucket.getLatencyP99(),
                        bucket.getLatencyMin(),
                        bucket.getLatencyMax(),
                        toJsonb(bucket.getBySource()),
                        toJsonb(bucket.getByEventType()),
                        toJsonb(bucket.getBySeverity()));
            } catch (Exception e) {
                logger.error("Failed to insert bucket {}: {}", bucket.getBucketStart(), e.getMessage());
            }
        }

        logger.info("Inserted {} metric buckets to TimescaleDB", buckets.size());
    }

    /**
     * Get aggregated metrics for a time window.
     */
    public MetricsBucket getMetrics(Duration window) {
        Instant now = Instant.now();
        Instant start = now.minus(window);

        return getMetricsBetween(start, now);
    }

    /**
     * Get aggregated metrics between two timestamps.
     */
    public MetricsBucket getMetricsBetween(Instant start, Instant end) {
        String sql = """
                SELECT
                    SUM(total_events) as total_events,
                    SUM(total_errors) as total_errors,
                    SUM(latency_sum) as latency_sum,
                    SUM(latency_count) as latency_count,
                    AVG(latency_p50) as latency_p50,
                    AVG(latency_p95) as latency_p95,
                    AVG(latency_p99) as latency_p99,
                    MIN(latency_min) as latency_min,
                    MAX(latency_max) as latency_max
                FROM metrics_buckets
                WHERE bucket_start >= ? AND bucket_start < ?
                """;

        try {
            return jdbcTemplate.queryForObject(sql, new MetricsBucketRowMapper(start, end),
                    Timestamp.from(start), Timestamp.from(end));
        } catch (Exception e) {
            logger.error("Failed to get metrics from TimescaleDB: {}", e.getMessage());
            return new MetricsBucket(start, end);
        }
    }

    /**
     * Get metrics using TimescaleDB's time_bucket function for efficient
     * aggregation.
     */
    public List<MetricsBucket> getMetricsByTimeBucket(Duration window, Duration bucketSize) {
        Instant now = Instant.now();
        Instant start = now.minus(window);

        String sql = """
                SELECT
                    time_bucket(?, bucket_start) as bucket,
                    SUM(total_events) as total_events,
                    SUM(total_errors) as total_errors,
                    SUM(latency_sum) as latency_sum,
                    SUM(latency_count) as latency_count,
                    AVG(latency_p95) as latency_p95,
                    MIN(latency_min) as latency_min,
                    MAX(latency_max) as latency_max
                FROM metrics_buckets
                WHERE bucket_start >= ? AND bucket_start < ?
                GROUP BY bucket
                ORDER BY bucket DESC
                """;

        try {
            String intervalStr = bucketSize.toMinutes() + " minutes";
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Timestamp bucketTs = rs.getTimestamp("bucket");
                MetricsBucket bucket = new MetricsBucket(
                        bucketTs.toInstant(),
                        bucketTs.toInstant().plus(bucketSize));
                bucket.setTotalEvents(rs.getLong("total_events"));
                bucket.setTotalErrors(rs.getLong("total_errors"));
                bucket.setLatencySum(rs.getLong("latency_sum"));
                bucket.setLatencyCount(rs.getLong("latency_count"));
                bucket.setLatencyP95(rs.getDouble("latency_p95"));
                bucket.setLatencyMin(rs.getObject("latency_min", Long.class));
                bucket.setLatencyMax(rs.getObject("latency_max", Long.class));
                return bucket;
            }, intervalStr, Timestamp.from(start), Timestamp.from(now));
        } catch (Exception e) {
            logger.error("Failed to get bucketed metrics: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Get event count in a specific time window (for time window metrics).
     */
    public long countEventsInWindow(Duration window) {
        Instant now = Instant.now();
        Instant start = now.minus(window);

        String sql = """
                SELECT COALESCE(SUM(total_events), 0)
                FROM metrics_buckets
                WHERE bucket_start >= ? AND bucket_start < ?
                """;

        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class,
                    Timestamp.from(start), Timestamp.from(now));
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Failed to count events: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Convert map to JSONB string.
     */
    private String toJsonb(Object map) {
        if (map == null) {
            return "{}";
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Row mapper for MetricsBucket.
     */
    private static class MetricsBucketRowMapper implements RowMapper<MetricsBucket> {
        private final Instant start;
        private final Instant end;

        public MetricsBucketRowMapper(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public MetricsBucket mapRow(ResultSet rs, int rowNum) throws SQLException {
            MetricsBucket bucket = new MetricsBucket(start, end);
            bucket.setTotalEvents(rs.getLong("total_events"));
            bucket.setTotalErrors(rs.getLong("total_errors"));
            bucket.setLatencySum(rs.getLong("latency_sum"));
            bucket.setLatencyCount(rs.getLong("latency_count"));
            bucket.setLatencyP50(rs.getDouble("latency_p50"));
            bucket.setLatencyP95(rs.getDouble("latency_p95"));
            bucket.setLatencyP99(rs.getDouble("latency_p99"));
            bucket.setLatencyMin(rs.getObject("latency_min", Long.class));
            bucket.setLatencyMax(rs.getObject("latency_max", Long.class));
            return bucket;
        }
    }
}
