-- Rule execution log table
CREATE TABLE rule_execution_log (
    id BIGSERIAL  PRIMARY KEY,

    -- rule reference
    rule_id BIGINT NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,

    -- Execution info
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time_ms INTEGER NOT NULL,

    -- Result
    executed_met BOOLEAN NOT NULL,
    threshold_value DOUBLE PRECISION,
    actual_value DOUBLE PRECISION,

    -- Details
    evaluation_result JSONB,
    error_message TEXT,

    -- Drools specifics
    facts_inserted INTEGER,
    rules_fires INTEGER,
    session_id VARCHAR(255)
);

CREATE INDEX idx_rule_execution_log_rule_id ON rule_execution_log(rule_id);
CREATE INDEX idx_rule_execution_log_executed_at ON rule_execution_log(executed_at);
CREATE INDEX idx_rule_execution_log_condition_met ON
rule_execution_log(condition_met);