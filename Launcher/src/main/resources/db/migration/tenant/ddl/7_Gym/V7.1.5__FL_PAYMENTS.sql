CREATE TABLE FL_PAYMENTS
(
    id                UUID PRIMARY KEY,
    amount            NUMERIC(12, 2) NOT NULL,
    paid_amount       NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    due_date          DATE           NOT NULL,
    paid_date         DATE,
    payment_method    VARCHAR(50)    NOT NULL DEFAULT 'NONE',
    transaction_id    VARCHAR(100),
    notes             VARCHAR(500),
    created_by        UUID           NOT NULL,
    created_date      TIMESTAMPTZ    NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ
);
