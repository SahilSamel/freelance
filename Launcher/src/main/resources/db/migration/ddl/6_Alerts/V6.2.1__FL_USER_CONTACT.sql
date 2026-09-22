CREATE TABLE IF NOT EXISTS freelance.FL_USER_CONTACT
(
    id                        UUID PRIMARY KEY,
    user_id                   UUID         NOT NULL,
    channel                   VARCHAR(20)  NOT NULL,
    contact_value             VARCHAR(255) NOT NULL,
    is_opted_out              BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    human_session_expires_at  TIMESTAMPTZ,
    created_by                UUID         NOT NULL,
    created_date              TIMESTAMPTZ  NOT NULL,
    last_updated_by           UUID,
    last_updated_date         TIMESTAMPTZ,
    CONSTRAINT uq_user_contact_channel UNIQUE (user_id, channel),
    CONSTRAINT fk_user_contact_user FOREIGN KEY (user_id) REFERENCES freelance.FL_USERS (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_contact_phone ON freelance.FL_USER_CONTACT (contact_value, channel);
CREATE INDEX IF NOT EXISTS idx_user_contact_user ON freelance.FL_USER_CONTACT (user_id);
CREATE INDEX IF NOT EXISTS idx_user_contact_session ON freelance.FL_USER_CONTACT (user_id, human_session_expires_at);
