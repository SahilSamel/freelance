CREATE TABLE FL_ERROR_MESSAGE
(
    error_id    VARCHAR(255) PRIMARY KEY,
    ui_message  TEXT NOT NULL,
    description VARCHAR(255)
);

CREATE INDEX idx_fl_error_map_id ON FL_ERROR_MESSAGE (error_id);