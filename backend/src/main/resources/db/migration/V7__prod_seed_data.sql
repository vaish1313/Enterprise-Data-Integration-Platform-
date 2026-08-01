-- V7__prod_seed_data.sql
-- Minimal seed data for the production demo environment

-- 1. Insert 1 Admin User (password is 'admin123' bcrypt encoded)
INSERT INTO users (username, password, email, role, status, created_at, updated_at) 
VALUES (
    'admin', 
    '$2a$12$K12x4.95aKxO2B2f52N.7eKx8B1xN/5z4B7o3n2qX8Z3s6y4l5CjG', 
    'admin@example.com', 
    'ROLE_ADMIN', 
    'ACTIVE', 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
) ON CONFLICT (username) DO NOTHING;

-- 2. Insert Sample Data Sources
INSERT INTO data_sources (name, type, url, username, password, status, created_at, updated_at)
VALUES 
    ('CRM Database', 'POSTGRESQL', 'jdbc:postgresql://crm-db.internal:5432/crm', 'crm_user', 'crm_pass', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Marketing API', 'REST_API', 'https://api.marketing.example.com/v1', 'api_key', 'secret', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- 3. Insert Sample Notifications
INSERT INTO notifications (title, message, type, read, created_at)
VALUES
    ('System Update', 'The system has been updated to the latest version.', 'INFO', false, CURRENT_TIMESTAMP),
    ('Deployment Successful', 'Enterprise Data Integration Platform deployed successfully.', 'SUCCESS', false, CURRENT_TIMESTAMP);
