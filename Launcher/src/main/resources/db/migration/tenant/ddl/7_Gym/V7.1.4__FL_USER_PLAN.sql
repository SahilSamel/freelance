CREATE TABLE FL_USER_PLAN
(
    id                UUID PRIMARY KEY,
    user_id           UUID        NOT NULL,
    plan_id           UUID        NOT NULL,
    start_date        DATE        NOT NULL,
    end_date          DATE        NOT NULL,
    created_by        UUID        NOT NULL,
    created_date      TIMESTAMPTZ NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ,
    CONSTRAINT fk_user_plan_user FOREIGN KEY (user_id) REFERENCES FL_USERS (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_plan_plan FOREIGN KEY (plan_id) REFERENCES FL_PLAN (id) ON DELETE RESTRICT
);
