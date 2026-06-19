CREATE TABLE system_configuration
(
    config_id    VARCHAR(255) PRIMARY KEY,
    config_value TEXT NOT NULL,
    description  VARCHAR(255)
);

CREATE TABLE error_message_mapping
(
    error_id    VARCHAR(255) PRIMARY KEY,
    ui_message  TEXT NOT NULL,
    description VARCHAR(255)
);

CREATE INDEX idx_sys_config_id ON system_configuration (config_id);
CREATE INDEX idx_error_map_id ON error_message_mapping (error_id);