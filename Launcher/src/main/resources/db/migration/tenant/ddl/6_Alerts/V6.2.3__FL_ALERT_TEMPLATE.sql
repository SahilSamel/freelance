CREATE TABLE IF NOT EXISTS FL_ALERT_TEMPLATE
(
    id                  UUID PRIMARY KEY,
    template_key        VARCHAR(255) NOT NULL UNIQUE,
    name                VARCHAR(255) NOT NULL,
    category            VARCHAR(100) NOT NULL,
    language            VARCHAR(10)  NOT NULL DEFAULT 'en',
    content             TEXT         NOT NULL,
    media_url           TEXT,
    media_type          VARCHAR(50),
    channel             VARCHAR(20)  NOT NULL DEFAULT 'WHATSAPP',
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    is_system           BOOLEAN      NOT NULL DEFAULT FALSE,
    required_attributes TEXT,
    meta_template_id    VARCHAR(255),
    priority            VARCHAR(20),
    created_by          UUID         NOT NULL,
    created_date        TIMESTAMPTZ  NOT NULL,
    last_updated_by     UUID,
    last_updated_date   TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_alert_template_key ON FL_ALERT_TEMPLATE (template_key);
CREATE INDEX IF NOT EXISTS idx_alert_template_status ON FL_ALERT_TEMPLATE (status);
