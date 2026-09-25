CREATE TABLE IF NOT EXISTS FL_ALERT_BUTTON
(
    id                UUID PRIMARY KEY,
    template_id       UUID         NOT NULL REFERENCES FL_ALERT_TEMPLATE(id) ON DELETE CASCADE,
    button_text       VARCHAR(255) NOT NULL,
    button_type       VARCHAR(50)  NOT NULL,
    payload           VARCHAR(255) NOT NULL,
    sequence_order    INT          NOT NULL DEFAULT 0,
    created_by        UUID         NOT NULL,
    created_date      TIMESTAMPTZ  NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_alert_button_template ON FL_ALERT_BUTTON (template_id);
CREATE INDEX IF NOT EXISTS idx_alert_button_payload ON FL_ALERT_BUTTON (payload);
