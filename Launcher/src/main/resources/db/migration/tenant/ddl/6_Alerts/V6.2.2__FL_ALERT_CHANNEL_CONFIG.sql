CREATE TABLE IF NOT EXISTS FL_ALERT_CHANNEL_CONFIG
(
    channel           VARCHAR(20) PRIMARY KEY,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    pause_reason      VARCHAR(255),
    created_by        UUID         NOT NULL,
    created_date      TIMESTAMPTZ  NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ
);

INSERT INTO FL_ALERT_CHANNEL_CONFIG (channel, is_active, pause_reason, created_by, created_date)
VALUES ('WHATSAPP', TRUE, NULL, '00000000-0000-0000-0000-000000000000', NOW())
ON CONFLICT (channel) DO NOTHING;
