CREATE TABLE IF NOT EXISTS freelance.FL_ALERT_BUTTON_REPLY
(
    id                   UUID PRIMARY KEY,
    button_id            UUID         NOT NULL REFERENCES freelance.FL_ALERT_BUTTON(id) ON DELETE CASCADE UNIQUE,
    reply_template_id    UUID         REFERENCES freelance.FL_ALERT_TEMPLATE(id) ON DELETE SET NULL,
    requires_human_agent BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by           UUID         NOT NULL,
    created_date         TIMESTAMPTZ  NOT NULL,
    last_updated_by      UUID,
    last_updated_date    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_button_reply_button ON freelance.FL_ALERT_BUTTON_REPLY (button_id);
