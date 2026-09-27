CREATE TABLE FL_USER_TYPE
(
    id                UUID PRIMARY KEY,
    code              VARCHAR(50)  NOT NULL UNIQUE,
    description       VARCHAR(255),
    created_by        UUID         NOT NULL,
    created_date      TIMESTAMPTZ  NOT NULL,
    last_updated_by   UUID,
    last_updated_date TIMESTAMPTZ
);

INSERT INTO FL_USER_TYPE (id, code, description, created_by, created_date)
VALUES ('00000000-0000-0000-0007-000000000001', 'ADMIN', 'Gym Administrator', '00000000-0000-0000-0000-000000000000', NOW()),
       ('00000000-0000-0000-0007-000000000002', 'MEMBER', 'Gym Member', '00000000-0000-0000-0000-000000000000', NOW())
ON CONFLICT (code) DO NOTHING;
