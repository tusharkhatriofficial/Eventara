-- Notification Log table

CREATE TABLE notification_log (
    id BIGSERIAL PRIMARY KEY,

    --References
    alert_id BIGINT REFERENCES alert_history(id) ON DELETE CASCADE,
    channel_id BIGINT REFERENCES notification_channels(id) ON DELETE SET NULL,

    -- Notification info
    channel_type VARCHAR(50) NOT NULL,
    recipient VARCHAR(500) NOT NULL,

    -- status
    status VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP,

    --content
    subject VARCHAR(500),
    message TEXT NOT NULL,

    --Response
    response_code INTEGER,
    response_body TEXT,
    error_message TEXT,

    -- Retry
    retry_count INTEGER DEFAULT 0,
    next_retry_at TIMESTAMP,

    -- Performance
    delivery_time_ms INTEGER,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRYING'))
);


CREATE INDEX idx_notification_log_alert_id ON notification_log(alert_id);
CREATE INDEX idx_notification_log_status ON notification_log(status);
CREATE INDEX idx_notification_log_sent_at ON notification_log(sent_at);