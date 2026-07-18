-- Core user identities are created before credentials add their foreign-key dependency.
CREATE TABLE FL_USERS
(
                       id UUID PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       created_at TIMESTAMP NOT NULL
);
