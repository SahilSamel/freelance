-- pgcrypto provides gen_random_uuid() for primary keys created inside PostgreSQL.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Credentials are separated from user profile data so authentication can evolve independently.
CREATE TABLE FL_USER_CREDENTIALS
(
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  user_id UUID NOT NULL,
                                  password VARCHAR(255) NOT NULL
);