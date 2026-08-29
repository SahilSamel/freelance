CREATE TABLE IF NOT EXISTS freelance.FL_SYSTEM_CONFIGURATION
(
    config_id         VARCHAR(255) PRIMARY KEY,
    config_value      TEXT         NOT NULL,
    description       VARCHAR(255),
    created_by        UUID         NOT NULL,
    created_date     TIMESTAMPTZ  NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_fl_sys_config_id ON freelance.FL_SYSTEM_CONFIGURATION (config_id);