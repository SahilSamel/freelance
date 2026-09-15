CREATE TABLE IF NOT EXISTS freelance.FL_ALERT_TEMPLATE_CATEGORY
(
    id           UUID PRIMARY KEY,
    category_key VARCHAR(100) NOT NULL UNIQUE,
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    priority     VARCHAR(20)  NOT NULL DEFAULT 'NORMAL'
);

CREATE INDEX IF NOT EXISTS idx_alert_template_category_key ON freelance.FL_ALERT_TEMPLATE_CATEGORY (category_key);
