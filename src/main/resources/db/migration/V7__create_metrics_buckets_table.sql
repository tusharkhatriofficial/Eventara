-- =============================================================================
-- V7: Create metrics_buckets table for distributed sliding window metrics
-- Using TimescaleDB for time-series optimization
-- =============================================================================

-- Enable TimescaleDB extension (idempotent)
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

-- =============================================================================
-- Metrics Buckets Table
-- Stores aggregated metrics for each time bucket
-- =============================================================================
CREATE TABLE IF NOT EXISTS metrics_buckets (
    id BIGSERIAL,
    bucket_start TIMESTAMPTZ NOT NULL,
    bucket_end TIMESTAMPTZ NOT NULL,
    
    -- Core counters
    total_events BIGINT DEFAULT 0,
    total_errors BIGINT DEFAULT 0,
    
    -- Latency aggregates (for calculating avg)
    latency_sum BIGINT DEFAULT 0,
    latency_count BIGINT DEFAULT 0,
    
    -- Percentile data (stored as array for accurate percentile calculation)
    latency_p50 DOUBLE PRECISION,
    latency_p95 DOUBLE PRECISION,
    latency_p99 DOUBLE PRECISION,
    latency_min BIGINT,
    latency_max BIGINT,
    
    -- Breakdown by source (JSONB for flexibility)
    -- Format: {"source_name": {"events": 100, "errors": 5, "latency_sum": 1000}}
    by_source JSONB DEFAULT '{}',
    
    -- Breakdown by event type (JSONB for flexibility)
    -- Format: {"event_type": {"count": 100, "latency_sum": 1000}}
    by_event_type JSONB DEFAULT '{}',
    
    -- Breakdown by severity
    by_severity JSONB DEFAULT '{}',
    
    -- Unique counts (approximate using HyperLogLog-style storage)
    unique_users_estimate INT DEFAULT 0,
    unique_sources_estimate INT DEFAULT 0,
    unique_event_types_estimate INT DEFAULT 0,
    
    -- Metadata
    created_at TIMESTAMPTZ DEFAULT NOW(),
    
    PRIMARY KEY (id, bucket_start)
);

-- =============================================================================
-- Convert to TimescaleDB Hypertable
-- This enables automatic time-based partitioning and optimizations
-- =============================================================================
SELECT create_hypertable('metrics_buckets', 'bucket_start', 
    chunk_time_interval => INTERVAL '1 hour',
    if_not_exists => TRUE
);

-- =============================================================================
-- Indexes for Query Performance
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_metrics_buckets_bucket_start 
ON metrics_buckets (bucket_start DESC);

-- =============================================================================
-- Retention Policy
-- Automatically delete data older than 30 days
-- =============================================================================
SELECT add_retention_policy('metrics_buckets', INTERVAL '30 days', if_not_exists => TRUE);

-- =============================================================================
-- Compression Policy
-- Compress chunks older than 7 days to save storage (90%+ compression)
-- =============================================================================
ALTER TABLE metrics_buckets SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = ''
);

SELECT add_compression_policy('metrics_buckets', INTERVAL '7 days', if_not_exists => TRUE);

-- =============================================================================
-- Continuous Aggregate: 1-minute rollups
-- Pre-aggregated view for faster queries over longer time ranges
-- =============================================================================
CREATE MATERIALIZED VIEW IF NOT EXISTS metrics_buckets_1min
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 minute', bucket_start) AS bucket_1min,
    SUM(total_events) AS total_events,
    SUM(total_errors) AS total_errors,
    SUM(latency_sum) AS latency_sum,
    SUM(latency_count) AS latency_count,
    AVG(latency_p95) AS latency_p95_avg,
    MAX(latency_max) AS latency_max,
    MIN(latency_min) AS latency_min
FROM metrics_buckets
GROUP BY bucket_1min
WITH NO DATA;

-- Refresh policy for continuous aggregate
SELECT add_continuous_aggregate_policy('metrics_buckets_1min',
    start_offset => INTERVAL '1 hour',
    end_offset => INTERVAL '1 minute',
    schedule_interval => INTERVAL '1 minute',
    if_not_exists => TRUE
);

-- =============================================================================
-- Comments for documentation
-- =============================================================================
COMMENT ON TABLE metrics_buckets IS 'Time-bucketed metrics for sliding window calculations';
COMMENT ON COLUMN metrics_buckets.bucket_start IS 'Start time of the bucket (10-second intervals)';
COMMENT ON COLUMN metrics_buckets.by_source IS 'JSONB breakdown of metrics per source';
COMMENT ON COLUMN metrics_buckets.by_event_type IS 'JSONB breakdown of metrics per event type';
