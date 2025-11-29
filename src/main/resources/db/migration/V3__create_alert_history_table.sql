CREATE TABLE alert_history(
    id BIGSERIAL PRIMARY KEY,

    --Rule reference
    rule_id BIGINT NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
    rule_name VARCHAR(255) NOT NULL,
    rule_version INTEGER NOT NULL,

    --Alert Info
    severity VARCHAR(20) NOT NULL,
    priority INTEGER NOT NULL,
    message TEXT NOT NULL,
    description TEXT,

    --values
    threshold_value DOUBLE PRECISION NOT NULL,
    actual_value DOUBLE PRECISION NOT NULL,

    --Lifecycle
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    triggered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    acknowledged_at TIMESTAMP,
    acknowledged_by VARCHAR(255),
    acknowledgment_notes TEXT,

    resolved_at TIMESTAMP,
    resolved_by VARCHAR(255),
    resolution_notes TEXT,
    resolution_type VARCHAR(50),

    expired_at TIMESTAMP,

    -- Context
    context JSONB NOT NULL,

    -- Notifications tracking
    notifications_sent JSONB,

    -- Tage
    tags TEXT[],

    -- Performance
    evaluation_time_ms INTEGER,

    CONSTRAINT chk_alert_status CHECK (
        status IN ('ACTIVE', 'ACKNOWLEDGED', 'RESOLVED', 'SUPPRESSED', 'EXPIRED', 'AUTO_RESOLVED')
    ),

    CONSTRAINT chk_alert_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),

    CONSTRAINT chk_resolution_type CHECK (resolution_type IN ('MANUAL', 'AUTO', 'TIMEOUT') OR resolution_type IS NULL)
);

CREATE INDEX idx_alert_history_rule_id ON alert_history(rule_id);
CREATE INDEX idx_alert_history_triggered_at ON alert_history(triggered_at);
CREATE INDEX idx_alert_history_status ON alert_history(status);
CREATE INDEX idx_alert_history_severity ON alert_history(severity);
CREATE INDEX idx_alert_history_context ON alert_history USING GIN(context);
CREATE INDEX idx_alert_history_tags ON alert_history USING GIN(tags);