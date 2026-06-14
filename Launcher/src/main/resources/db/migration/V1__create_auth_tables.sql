-- pgcrypto provides gen_random_uuid() for primary keys created inside PostgreSQL.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Credentials are separated from user profile data so authentication can evolve independently.
CREATE TABLE user_credentials (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  user_id UUID NOT NULL,
                                  password VARCHAR(255) NOT NULL
);

-- Sessions store issued login sessions and their expiry boundary.
CREATE TABLE sessions (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          session_id VARCHAR(255) NOT NULL UNIQUE,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          expires_at TIMESTAMP
);
