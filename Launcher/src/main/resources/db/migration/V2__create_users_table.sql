-- Core user identities are created before credentials add their foreign-key dependency.
CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       created_at TIMESTAMP NOT NULL
);

-- Link authentication credentials back to the owning user identity.
ALTER TABLE user_credentials
    ADD CONSTRAINT fk_user_credentials_user
        FOREIGN KEY (user_id)
            REFERENCES users(id);
