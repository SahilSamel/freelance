CREATE TABLE FL_USER_PLAN_PAYMENT
(
    id                UUID PRIMARY KEY,
    user_plan_id      UUID        NOT NULL,
    payment_id        UUID        NOT NULL,
    created_by        UUID        NOT NULL,
    created_date      TIMESTAMPTZ NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ,
    CONSTRAINT fk_upp_user_plan FOREIGN KEY (user_plan_id) REFERENCES FL_USER_PLAN (id) ON DELETE CASCADE,
    CONSTRAINT fk_upp_payment FOREIGN KEY (payment_id) REFERENCES FL_PAYMENTS (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_plan_payment UNIQUE (user_plan_id, payment_id)
);
