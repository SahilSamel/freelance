CREATE TABLE FL_MEMBER_REFERRAL
(
    id                UUID PRIMARY KEY,
    referrer_id       UUID        NOT NULL,
    referee_id        UUID        NOT NULL,
    referral_date     DATE        NOT NULL,
    notes             TEXT,
    created_by        UUID        NOT NULL,
    created_date      TIMESTAMPTZ NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ,
    CONSTRAINT fk_referral_referrer FOREIGN KEY (referrer_id) REFERENCES FL_USERS (id) ON DELETE CASCADE,
    CONSTRAINT fk_referral_referee FOREIGN KEY (referee_id) REFERENCES FL_USERS (id) ON DELETE CASCADE,
    CONSTRAINT uq_member_referral_referee UNIQUE (referee_id)
);
