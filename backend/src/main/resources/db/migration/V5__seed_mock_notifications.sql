-- V5__seed_mock_notifications.sql

-- Insert mock notifications for the 'admin' user so the UI has initial data to display
INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at, related_entity_type)
SELECT gen_random_uuid(), id, 'SUCCESS', 'Sync Completed', 'Data source "CRM Export" synced 12,450 records successfully.', false, NOW() - INTERVAL '2 minutes', 'SYNC_JOB'
FROM users WHERE username = 'admin' LIMIT 1;

INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at, related_entity_type)
SELECT gen_random_uuid(), id, 'ERROR', 'Ingestion Failed', 'CSV upload for "Sales Q2" failed: invalid column format.', false, NOW() - INTERVAL '15 minutes', 'INGESTION_JOB'
FROM users WHERE username = 'admin' LIMIT 1;

INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at, related_entity_type)
SELECT gen_random_uuid(), id, 'INFO', 'Scheduler Running', 'Scheduled sync started for 3 active data sources.', true, NOW() - INTERVAL '1 hour', null
FROM users WHERE username = 'admin' LIMIT 1;

INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at, related_entity_type)
SELECT gen_random_uuid(), id, 'WARNING', 'Sync Partial', '42 records failed validation during sync of "ERP Connector".', true, NOW() - INTERVAL '3 hours', 'SYNC_JOB'
FROM users WHERE username = 'admin' LIMIT 1;
