CREATE TABLE IF NOT EXISTS freelance.FL_USER_ROLE (
    user_id           UUID         NOT NULL,
    role_id           UUID         NOT NULL,
    created_by        UUID         NOT NULL,
    created_date      TIMESTAMPTZ  NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES freelance.FL_USERS (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES freelance.FL_ROLE (id) ON DELETE CASCADE
);
