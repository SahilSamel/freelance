CREATE TABLE FL_MEMBER_CREDIT
(
    id                UUID PRIMARY KEY,
    user_id           UUID           NOT NULL,
    balance           DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    notes             TEXT,
    created_by        UUID           NOT NULL,
    created_date      TIMESTAMPTZ    NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ,
    CONSTRAINT fk_credit_user FOREIGN KEY (user_id) REFERENCES FL_USERS (id) ON DELETE CASCADE,
    CONSTRAINT uq_member_credit_user UNIQUE (user_id)
);
