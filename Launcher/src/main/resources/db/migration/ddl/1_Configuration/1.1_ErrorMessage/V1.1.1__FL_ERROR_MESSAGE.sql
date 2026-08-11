CREATE TABLE FL_ERROR_MESSAGE
(
    error_id          VARCHAR(255) PRIMARY KEY,
    error_message     TEXT         NOT NULL,
    description       VARCHAR(255),
    created_by        UUID         NOT NULL,
    creation_date     TIMESTAMPTZ  NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ
);

CREATE INDEX idx_fl_error_map_id ON FL_ERROR_MESSAGE (error_id);