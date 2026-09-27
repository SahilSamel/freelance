CREATE TABLE FL_USERS
(
    id                UUID PRIMARY KEY,
    username          VARCHAR(255) NOT NULL UNIQUE,
    password          VARCHAR(255) NOT NULL,
    first_name        VARCHAR(100),
    last_name         VARCHAR(100),
    created_by        UUID         NOT NULL,
    created_date      TIMESTAMPTZ  NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ
);