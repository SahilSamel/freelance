CREATE TABLE freelance.FL_USER_CREDENTIALS
(
    id                UUID PRIMARY KEY,
    password          VARCHAR(255) NOT NULL,
    created_by        UUID         NOT NULL,
    created_date      TIMESTAMPTZ  NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ
);

ALTER TABLE freelance.FL_USERS
    ADD CONSTRAINT fk_fl_users_credentials
        FOREIGN KEY (user_credentials_id)
            REFERENCES freelance.FL_USER_CREDENTIALS (id)
            ON DELETE SET NULL;

CREATE INDEX idx_fl_users_credentials_id
    ON freelance.FL_USERS (user_credentials_id);