CREATE TABLE IF NOT EXISTS FL_ALERT_MESSAGE
(
    id                UUID PRIMARY KEY,
    user_id           UUID         NOT NULL,
    channel           VARCHAR(20)  NOT NULL,
    direction         VARCHAR(10)  NOT NULL,
    message_type      VARCHAR(20)  NOT NULL,
    content           TEXT         NOT NULL,
    template_id       UUID,
    meta_message_id   VARCHAR(255),
    status            VARCHAR(20)  NOT NULL,
    error_code        VARCHAR(100),
    raw_error_details TEXT,
    timestamp         TIMESTAMPTZ  NOT NULL,
    media_url         TEXT,
    media_id          VARCHAR(255),
    media_type        VARCHAR(50),
    created_by        UUID         NOT NULL,
    created_date      TIMESTAMPTZ  NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ,
    CONSTRAINT fk_alert_message_template FOREIGN KEY (template_id) REFERENCES FL_ALERT_TEMPLATE (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_alert_message_user ON FL_ALERT_MESSAGE (user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_alert_message_wamid ON FL_ALERT_MESSAGE (meta_message_id);
CREATE INDEX IF NOT EXISTS idx_alert_message_template_id ON FL_ALERT_MESSAGE (template_id);
CREATE INDEX IF NOT EXISTS idx_alert_message_media_id ON FL_ALERT_MESSAGE (media_id);
