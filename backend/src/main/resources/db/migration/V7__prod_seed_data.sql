-- V7__prod_seed_data.sql
-- Minimal seed data for the production demo environment

-- 1. Insert 1 Admin User (password is 'admin123' bcrypt encoded)
INSERT INTO users (username, password, email, role, enabled, created_at, updated_at) 
VALUES (
    'admin', 
    '$2b$12$d9TuIdSbaX57kUaPplpmK.Ri8deyQTx2aVpLtycC8kJtFt5p4Z8om', 
    'admin@example.com', 
    'ADMIN', 
    true, 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
) ON CONFLICT (username) DO NOTHING;

-- 2. Insert Sample Data Sources
-- Since data_sources doesn't have a unique constraint, we use WHERE NOT EXISTS to avoid duplicates
INSERT INTO data_sources (name, source_type, connection_details, status, created_at, updated_at)
SELECT 'CRM Database', 'DATABASE', '{"url": "jdbc:postgresql://crm-db.internal:5432/crm", "username": "crm_user", "password": "crm_pass"}'::jsonb, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM data_sources WHERE name = 'CRM Database');

INSERT INTO data_sources (name, source_type, connection_details, status, created_at, updated_at)
SELECT 'Marketing API', 'REST_API', '{"url": "https://api.marketing.example.com/v1", "api_key": "api_key", "secret": "secret"}'::jsonb, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM data_sources WHERE name = 'Marketing API');

-- 3. Insert Sample Notifications
-- Fetches the newly inserted admin user's ID to satisfy the foreign key constraint
INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at)
SELECT 
    gen_random_uuid(), 
    u.id, 
    'INFO', 
    'System Update', 
    'The system has been updated to the latest version.', 
    false, 
    CURRENT_TIMESTAMP
FROM users u WHERE u.username = 'admin'
AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.title = 'System Update' AND n.user_id = u.id);

INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at)
SELECT 
    gen_random_uuid(), 
    u.id, 
    'SUCCESS', 
    'Deployment Successful', 
    'Enterprise Data Integration Platform deployed successfully.', 
    false, 
    CURRENT_TIMESTAMP
FROM users u WHERE u.username = 'admin'
AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.title = 'Deployment Successful' AND n.user_id = u.id);
