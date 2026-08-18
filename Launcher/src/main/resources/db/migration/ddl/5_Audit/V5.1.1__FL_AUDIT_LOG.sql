CREATE TABLE IF NOT EXISTS freelance.FL_AUDIT_LOG
(
    id                UUID PRIMARY KEY,
    timestamp         TIMESTAMPTZ   NOT NULL,
    trace_id          VARCHAR(255)  NOT NULL,
    user_id           UUID,
    tenant_id         VARCHAR(255),
    http_method       VARCHAR(10)   NOT NULL,
    endpoint          VARCHAR(1024) NOT NULL,
    status_code       INTEGER       NOT NULL,
    duration_ms       BIGINT        NOT NULL,
    ip_address        VARCHAR(45),
    user_agent        TEXT,
    auth_mechanism    VARCHAR(50),
    created_by        UUID,
    created_date      TIMESTAMPTZ   NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_fl_audit_log_timestamp
    ON freelance.FL_AUDIT_LOG (timestamp);

CREATE INDEX IF NOT EXISTS idx_fl_audit_log_trace_id
    ON freelance.FL_AUDIT_LOG (trace_id);

CREATE INDEX IF NOT EXISTS idx_fl_audit_log_user_id
    ON freelance.FL_AUDIT_LOG (user_id);

CREATE INDEX IF NOT EXISTS idx_fl_audit_log_tenant_id
    ON freelance.FL_AUDIT_LOG (tenant_id);
