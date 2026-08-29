CREATE TABLE IF NOT EXISTS freelance.FL_ERROR_MESSAGE
(
    error_id          VARCHAR(255) PRIMARY KEY,
    status_code       INT          NOT NULL,
    error_message     TEXT         NOT NULL,
    description       VARCHAR(255),
    created_by        UUID         NOT NULL,
    created_date     TIMESTAMPTZ  NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_fl_error_map_id ON freelance.FL_ERROR_MESSAGE (error_id);