-- Notification channels table
CREATE TABLE notifications_channels (
    id BIGSERIAL PRIMARY KEY,

    -- Channel info
    channel_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT true,

    -- Configuration
    config JSONB NOT NULL,

    -- Rate Limiting
    rate_limit_per_minute INTEGER DEFAULT 60,
    rate_limit_per_hour INTEGER DEFAULT 500,

    -- Status
    last_used_at TIMESTAMP,
    total_sent INTEGER DEFAULT 0,
    total_failed INTEGER DEFAULT 0,

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),

    CONSTRAINT chk_channel_type CHECK (channel_type IN ('EMAIL', 'SLACK', 'WEBHOOK', 'PAGERDUTY', 'SMS'))

);

CREATE INDEX idx_notification_channels_type ON notification_channels(channel_type);
CREATE INDEX idx_notification_channels_type ON notification_channels(channel_type);