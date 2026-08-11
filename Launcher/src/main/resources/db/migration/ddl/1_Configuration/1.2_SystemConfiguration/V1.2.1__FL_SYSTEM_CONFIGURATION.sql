CREATE TABLE FL_SYSTEM_CONFIGURATION
(
    config_id         VARCHAR(255) PRIMARY KEY,
    config_value      TEXT         NOT NULL,
    description       VARCHAR(255),
    created_by        UUID         NOT NULL,
    creation_date     TIMESTAMPTZ  NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ
);

CREATE INDEX idx_fl_sys_config_id ON FL_SYSTEM_CONFIGURATION (config_id);