CREATE TABLE FL_USER_USER_TYPE
(
    id                UUID PRIMARY KEY,
    user_id           UUID         NOT NULL,
    user_type_id      UUID         NOT NULL,
    created_by        UUID         NOT NULL,
    created_date      TIMESTAMPTZ  NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ,
    CONSTRAINT fk_user_user_type_user FOREIGN KEY (user_id) REFERENCES FL_USERS (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_user_type_type FOREIGN KEY (user_type_id) REFERENCES FL_USER_TYPE (id) ON DELETE RESTRICT,
    CONSTRAINT uq_user_user_type UNIQUE (user_id, user_type_id)
);
