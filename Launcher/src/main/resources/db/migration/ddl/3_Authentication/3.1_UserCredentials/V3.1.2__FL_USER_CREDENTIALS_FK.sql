-- Link authentication credentials back to the owning user identity.
ALTER TABLE FL_USER_CREDENTIALS
    ADD CONSTRAINT fk_fl_user_credentials_user
        FOREIGN KEY (user_id)
            REFERENCES FL_USERS (id);