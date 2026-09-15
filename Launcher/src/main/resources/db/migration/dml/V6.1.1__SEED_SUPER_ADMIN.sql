-- 1. Ensure SUPER_ADMIN system role exists in FL_ROLE
INSERT INTO freelance.FL_ROLE (id, name, description, is_system, created_by, created_date, last_updated_by, last_updated_date)
VALUES (
    '33601cb2-b85d-36ae-b2b0-6676c95c9839',
    'SUPER_ADMIN',
    'System generated role for SUPER_ADMIN',
    TRUE,
    '00000000-0000-0000-0000-000000000000',
    CURRENT_TIMESTAMP,
    '00000000-0000-0000-0000-000000000000',
    CURRENT_TIMESTAMP
)
ON CONFLICT (name) DO NOTHING;

-- 2. Insert Super Admin user if absent (password: Welcome@1)
INSERT INTO freelance.FL_USERS (id, username, password, created_by, created_date, last_updated_by, last_updated_date)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'superadmin',
    '$2a$12$KndVHQG4kfm7hiAId0JkJO9mH6CWKC5Jtd0tqV96.aHU2Jp0HjkRS',
    '00000000-0000-0000-0000-000000000000',
    CURRENT_TIMESTAMP,
    '00000000-0000-0000-0000-000000000000',
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;

-- 3. Map superadmin user to SUPER_ADMIN role if absent
INSERT INTO freelance.FL_USER_ROLE (user_id, role_id, created_by, created_date, last_updated_by, last_updated_date)
SELECT
    u.id,
    r.id,
    '00000000-0000-0000-0000-000000000000',
    CURRENT_TIMESTAMP,
    '00000000-0000-0000-0000-000000000000',
    CURRENT_TIMESTAMP
FROM freelance.FL_USERS u
CROSS JOIN freelance.FL_ROLE r
WHERE u.username = 'superadmin'
  AND r.name = 'SUPER_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Seed default WhatsApp contact for superadmin
INSERT INTO freelance.FL_USER_CONTACT (
    id,
    user_id,
    channel,
    contact_value,
    is_opted_out,
    is_active,
    human_session_expires_at,
    created_by,
    created_date,
    last_updated_by,
    last_updated_date
) VALUES (
             'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d',
             '00000000-0000-0000-0000-000000000001',
             'WHATSAPP',
             '918450932085',
             FALSE,
             TRUE,
             NULL,
             '00000000-0000-0000-0000-000000000000',
             CURRENT_TIMESTAMP,
             '00000000-0000-0000-0000-000000000000',
             CURRENT_TIMESTAMP
         ) ON CONFLICT (user_id, channel) DO NOTHING;