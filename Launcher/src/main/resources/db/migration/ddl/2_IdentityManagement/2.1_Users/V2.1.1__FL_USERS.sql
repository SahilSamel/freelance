CREATE TABLE FL_USERS
(
    id         UUID PRIMARY KEY,
    username   VARCHAR(255) NOT NULL UNIQUE,
    email      VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL,
    user_id    UUID UNIQUE,
    CONSTRAINT fk_user_credentials_user_id
        FOREIGN KEY (user_id) REFERENCES FL_USER_CREDENTIALS (id)
);