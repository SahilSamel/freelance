CREATE TABLE freelance.FL_USERS
(
    id                  UUID PRIMARY KEY,
    username            VARCHAR(255) NOT NULL UNIQUE,
    email               VARCHAR(255) NOT NULL UNIQUE,
    user_credentials_id UUID UNIQUE,
    created_by          UUID         NOT NULL,
    created_date        TIMESTAMPTZ  NOT NULL,
    last_updated_by     UUID,
    last_updated_date   TIMESTAMPTZ
);