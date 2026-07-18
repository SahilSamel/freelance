CREATE TABLE FL_SYSTEM_CONFIGURATION
(
    config_id    VARCHAR(255) PRIMARY KEY,
    config_value TEXT NOT NULL,
    description  VARCHAR(255)
);

CREATE INDEX idx_fl_sys_config_id ON FL_SYSTEM_CONFIGURATION (config_id);