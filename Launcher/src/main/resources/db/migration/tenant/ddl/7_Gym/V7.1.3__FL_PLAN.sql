CREATE TABLE FL_PLAN
(
    id                UUID PRIMARY KEY,
    name              VARCHAR(100)   NOT NULL UNIQUE,
    description       VARCHAR(500),
    default_price     NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    default_duration  INTEGER        NOT NULL DEFAULT 30,
    is_active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_by        UUID           NOT NULL,
    created_date      TIMESTAMPTZ    NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ
);
