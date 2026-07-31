-- =============================================================
-- EDIP — Seed Data  (verified against live Supabase schema)
-- Re-runnable: deletes all rows first, then re-inserts.
-- Run AFTER fix-schema.sql has been applied.
-- =============================================================

-- ─────────────────────────────────────────────────────────────
-- 0.  SAFE CLEANUP  (FK-safe delete order)
-- ─────────────────────────────────────────────────────────────
DELETE FROM audit_logs;
DELETE FROM sync_jobs;
DELETE FROM ingestion_records;
DELETE FROM ingestion_jobs;
DELETE FROM transformation_rules;
DELETE FROM refresh_tokens;
DELETE FROM data_sources;
DELETE FROM users;

-- ─────────────────────────────────────────────────────────────
-- 1.  USERS
-- Confirmed columns: id, created_at, updated_at,
--   username, email, password, role, enabled,
--   first_name, last_name
-- Password hash = BCrypt(12) of "Admin@1234"
-- ─────────────────────────────────────────────────────────────
INSERT INTO users (id, created_at, updated_at, username, email, password, role, enabled, first_name, last_name) VALUES
('aaaaaaaa-0001-0000-0000-000000000001', NOW()-INTERVAL '60 days', NOW(), 'admin',    'admin@company.com',    '$2a$12$HmqDGK8sKa6zUJSmkFt8uOVy82n0jSc2C9KKKQtT5AutoB2qHJQKu', 'ADMIN',    true,  'Admin',    'User'),
('aaaaaaaa-0002-0000-0000-000000000002', NOW()-INTERVAL '55 days', NOW(), 'analyst',  'analyst@company.com',  '$2a$12$HmqDGK8sKa6zUJSmkFt8uOVy82n0jSc2C9KKKQtT5AutoB2qHJQKu', 'ANALYST',  true,  'Sarah',    'Mitchell'),
('aaaaaaaa-0003-0000-0000-000000000003', NOW()-INTERVAL '50 days', NOW(), 'operator', 'operator@company.com', '$2a$12$HmqDGK8sKa6zUJSmkFt8uOVy82n0jSc2C9KKKQtT5AutoB2qHJQKu', 'OPERATOR', true,  'James',    'Carter'),
('aaaaaaaa-0004-0000-0000-000000000004', NOW()-INTERVAL '45 days', NOW(), 'alice',    'alice@company.com',    '$2a$12$HmqDGK8sKa6zUJSmkFt8uOVy82n0jSc2C9KKKQtT5AutoB2qHJQKu', 'ANALYST',  true,  'Alice',    'Johnson'),
('aaaaaaaa-0005-0000-0000-000000000005', NOW()-INTERVAL '40 days', NOW(), 'bob',      'bob@company.com',      '$2a$12$HmqDGK8sKa6zUJSmkFt8uOVy82n0jSc2C9KKKQtT5AutoB2qHJQKu', 'OPERATOR', true,  'Bob',      'Williams'),
('aaaaaaaa-0006-0000-0000-000000000006', NOW()-INTERVAL '30 days', NOW(), 'diana',    'diana@company.com',    '$2a$12$HmqDGK8sKa6zUJSmkFt8uOVy82n0jSc2C9KKKQtT5AutoB2qHJQKu', 'ANALYST',  true,  'Diana',    'Prince'),
('aaaaaaaa-0007-0000-0000-000000000007', NOW()-INTERVAL '20 days', NOW(), 'charlie',  'charlie@company.com',  '$2a$12$HmqDGK8sKa6zUJSmkFt8uOVy82n0jSc2C9KKKQtT5AutoB2qHJQKu', 'OPERATOR', false, 'Charlie',  'Brown');

-- ─────────────────────────────────────────────────────────────
-- 2.  DATA SOURCES
-- Confirmed columns: id, created_at, updated_at,
--   name, source_type, connection_details, status,
--   description, created_by
-- ─────────────────────────────────────────────────────────────
INSERT INTO data_sources (id, created_at, updated_at, name, source_type, connection_details, status, description, created_by) VALUES
('bbbbbbbb-0001-0000-0000-000000000001', NOW()-INTERVAL '58 days', NOW(), 'CRM PostgreSQL',        'DATABASE', '{"host":"crm-db.internal","port":"5432","database":"crm_prod"}',          'ACTIVE',   'Main CRM database — customer profiles and orders',       'admin'),
('bbbbbbbb-0002-0000-0000-000000000002', NOW()-INTERVAL '55 days', NOW(), 'Sales CSV Export',      'CSV',      '{"delimiter":",","encoding":"UTF-8","hasHeader":"true"}',                'ACTIVE',   'Weekly sales export from ERP system',                    'admin'),
('bbbbbbbb-0003-0000-0000-000000000003', NOW()-INTERVAL '52 days', NOW(), 'Marketing REST API',    'REST_API', '{"baseUrl":"https://api.marketing.internal/v2","authType":"Bearer"}',    'ACTIVE',   'Marketing platform API — campaign metrics',               'analyst'),
('bbbbbbbb-0004-0000-0000-000000000004', NOW()-INTERVAL '48 days', NOW(), 'HR Database',           'DATABASE', '{"host":"hr-db.internal","port":"5432","database":"hr_system"}',          'ACTIVE',   'Human resources — employee records and payroll',          'admin'),
('bbbbbbbb-0005-0000-0000-000000000005', NOW()-INTERVAL '45 days', NOW(), 'Legacy ERP Connector',  'REST_API', '{"baseUrl":"https://erp-legacy.internal/api/v1","authType":"Basic"}',    'INACTIVE', 'Legacy ERP — being decommissioned Q3 2026',               'admin'),
('bbbbbbbb-0006-0000-0000-000000000006', NOW()-INTERVAL '40 days', NOW(), 'Product Catalog CSV',   'CSV',      '{"delimiter":";","encoding":"UTF-8","hasHeader":"true"}',                'ACTIVE',   'Product catalog and pricing from PIM system',             'analyst'),
('bbbbbbbb-0007-0000-0000-000000000007', NOW()-INTERVAL '35 days', NOW(), 'Finance REST API',      'REST_API', '{"baseUrl":"https://finance.internal/v2","authType":"OAuth2"}',           'ACTIVE',   'Finance system — invoices and GL entries',                'admin'),
('bbbbbbbb-0008-0000-0000-000000000008', NOW()-INTERVAL '30 days', NOW(), 'Warehouse Database',    'DATABASE', '{"host":"wh-db.internal","port":"5432","database":"warehouse"}',          'ERROR',    'Warehouse inventory — connection issues since 2026-05-20','admin'),
('bbbbbbbb-0009-0000-0000-000000000009', NOW()-INTERVAL '25 days', NOW(), 'Support Tickets API',   'REST_API', '{"baseUrl":"https://support.internal/api/v3","authType":"ApiKey"}',      'ACTIVE',   'Customer support ticket system integration',              'analyst'),
('bbbbbbbb-0010-0000-0000-000000000010', NOW()-INTERVAL '20 days', NOW(), 'Analytics Warehouse',   'DATABASE', '{"host":"analytics-dw.internal","port":"5432","database":"analytics_dw"}','INACTIVE', 'Target analytics DW — write destination',                'admin');

-- ─────────────────────────────────────────────────────────────
-- 3.  TRANSFORMATION RULES
-- Confirmed columns: id, created_at, updated_at,
--   name, description, data_source_id, transformation_type,
--   source_field, target_field, default_value, extra_config,
--   active (NOT NULL), execution_order, created_by
-- ─────────────────────────────────────────────────────────────
INSERT INTO transformation_rules (id, created_at, updated_at, name, description, data_source_id, transformation_type, source_field, target_field, default_value, extra_config, active, execution_order, created_by) VALUES
(gen_random_uuid(), NOW()-INTERVAL '57 days', NOW(), 'Uppercase Email',       'Normalise email to uppercase',                    'bbbbbbbb-0001-0000-0000-000000000001', 'UPPERCASE',      'email',        'email',        NULL,         NULL,         true,  1, 'admin'),
(gen_random_uuid(), NOW()-INTERVAL '57 days', NOW(), 'Trim Customer Name',    'Strip whitespace from customer_name',             'bbbbbbbb-0001-0000-0000-000000000001', 'TRIM',           'customer_name','customer_name',NULL,         NULL,         true,  2, 'admin'),
(gen_random_uuid(), NOW()-INTERVAL '56 days', NOW(), 'Full Name Concat',      'Combine first_name + last_name into full_name',   'bbbbbbbb-0001-0000-0000-000000000001', 'CONCAT',         'first_name',   'full_name',    ' ',          'last_name',  true,  3, 'analyst'),
(gen_random_uuid(), NOW()-INTERVAL '56 days', NOW(), 'Default Country',       'Set country to UNKNOWN when missing',             'bbbbbbbb-0001-0000-0000-000000000001', 'DEFAULT_VALUE',  'country',      'country',      'UNKNOWN',    NULL,         true,  4, 'analyst'),
(gen_random_uuid(), NOW()-INTERVAL '54 days', NOW(), 'Normalise Sale Date',   'Convert sale_date dd/MM/yyyy to yyyy-MM-dd',      'bbbbbbbb-0002-0000-0000-000000000002', 'DATE_FORMAT',    'sale_date',    'sale_date_iso','yyyy-MM-dd', 'dd/MM/yyyy', true,  1, 'analyst'),
(gen_random_uuid(), NOW()-INTERVAL '54 days', NOW(), 'Lowercase Product Code','Lowercase product SKU for consistent matching',   'bbbbbbbb-0002-0000-0000-000000000002', 'LOWERCASE',      'product_code', 'product_code', NULL,         NULL,         true,  2, 'analyst'),
(gen_random_uuid(), NOW()-INTERVAL '53 days', NOW(), 'Default Region',        'Fallback region to UNKNOWN when blank',           'bbbbbbbb-0002-0000-0000-000000000002', 'DEFAULT_VALUE',  'region',       'region',       'UNKNOWN',    NULL,         true,  3, 'admin'),
(gen_random_uuid(), NOW()-INTERVAL '51 days', NOW(), 'Map Campaign ID',       'Direct map campaignId to campaign_id',            'bbbbbbbb-0003-0000-0000-000000000003', 'DIRECT_MAPPING', 'campaignId',   'campaign_id',  NULL,         NULL,         true,  1, 'analyst'),
(gen_random_uuid(), NOW()-INTERVAL '51 days', NOW(), 'Uppercase Channel',     'Normalise marketing channel to uppercase',        'bbbbbbbb-0003-0000-0000-000000000003', 'UPPERCASE',      'channel',      'channel',      NULL,         NULL,         true,  2, 'analyst'),
(gen_random_uuid(), NOW()-INTERVAL '47 days', NOW(), 'Uppercase Dept Code',   'Normalise dept_code to uppercase',                'bbbbbbbb-0004-0000-0000-000000000004', 'UPPERCASE',      'dept_code',    'dept_code',    NULL,         NULL,         true,  1, 'admin'),
(gen_random_uuid(), NOW()-INTERVAL '47 days', NOW(), 'Trim Job Title',        'Remove extra whitespace from job_title',          'bbbbbbbb-0004-0000-0000-000000000004', 'TRIM',           'job_title',    'job_title',    NULL,         NULL,         true,  2, 'admin'),
(gen_random_uuid(), NOW()-INTERVAL '34 days', NOW(), 'Uppercase Currency',    'Normalise currency to ISO 4217 uppercase',        'bbbbbbbb-0007-0000-0000-000000000007', 'UPPERCASE',      'currency',     'currency',     NULL,         NULL,         true,  1, 'analyst'),
(gen_random_uuid(), NOW()-INTERVAL '34 days', NOW(), 'Map Invoice Reference', 'Direct map invoiceRef to invoice_ref',            'bbbbbbbb-0007-0000-0000-000000000007', 'DIRECT_MAPPING', 'invoiceRef',   'invoice_ref',  NULL,         NULL,         true,  2, 'analyst'),
(gen_random_uuid(), NOW()-INTERVAL '39 days', NOW(), 'Map Unit Price',        'Direct map unit_price to price',                  'bbbbbbbb-0006-0000-0000-000000000006', 'DIRECT_MAPPING', 'unit_price',   'price',        NULL,         NULL,         true,  1, 'analyst'),
(gen_random_uuid(), NOW()-INTERVAL '20 days', NOW(), 'Global Trim Values',    'Global fallback — trim value field on any source',NULL,                                  'TRIM',           'value',        'value',        NULL,         NULL,         false, 0, 'admin');

-- ─────────────────────────────────────────────────────────────
-- 4.  INGESTION JOBS
-- Confirmed columns: id, created_at, updated_at,
--   data_source_id, status, ingestion_type, file_name,
--   total_records, records_processed, records_failed,
--   started_at, completed_at, error_message, triggered_by
-- ─────────────────────────────────────────────────────────────
INSERT INTO ingestion_jobs (id, created_at, updated_at, data_source_id, status, ingestion_type, file_name, total_records, records_processed, records_failed, started_at, completed_at, error_message, triggered_by) VALUES
('cccccccc-0001-0000-0000-000000000001', NOW()-INTERVAL '57 days', NOW(), 'bbbbbbbb-0001-0000-0000-000000000001', 'COMPLETED', 'SCHEDULED', NULL,                       45200, 45200,  0,    NOW()-INTERVAL '57 days', NOW()-INTERVAL '57 days'+INTERVAL '4 min 12 sec', NULL, 'SCHEDULER'),
('cccccccc-0002-0000-0000-000000000002', NOW()-INTERVAL '43 days', NOW(), 'bbbbbbbb-0001-0000-0000-000000000001', 'COMPLETED', 'SCHEDULED', NULL,                       46800, 46800,  0,    NOW()-INTERVAL '43 days', NOW()-INTERVAL '43 days'+INTERVAL '4 min 38 sec', NULL, 'SCHEDULER'),
('cccccccc-0003-0000-0000-000000000003', NOW()-INTERVAL '15 days', NOW(), 'bbbbbbbb-0001-0000-0000-000000000001', 'COMPLETED', 'SCHEDULED', NULL,                       47500, 47500,  0,    NOW()-INTERVAL '15 days', NOW()-INTERVAL '15 days'+INTERVAL '4 min 55 sec', NULL, 'SCHEDULER'),
('cccccccc-0004-0000-0000-000000000004', NOW()-INTERVAL '54 days', NOW(), 'bbbbbbbb-0002-0000-0000-000000000002', 'COMPLETED', 'CSV',       'sales_q1_2026.csv',        12800, 12750,  50,   NOW()-INTERVAL '54 days', NOW()-INTERVAL '54 days'+INTERVAL '1 min 48 sec', NULL, 'analyst'),
('cccccccc-0005-0000-0000-000000000005', NOW()-INTERVAL '40 days', NOW(), 'bbbbbbbb-0002-0000-0000-000000000002', 'FAILED',    'CSV',       'sales_q2_bad_format.csv',  5000,  0,      5000, NOW()-INTERVAL '40 days', NOW()-INTERVAL '40 days'+INTERVAL '8 sec',         'Invalid column format: expected numeric in column "amount", got "N/A" at row 42', 'analyst'),
('cccccccc-0006-0000-0000-000000000006', NOW()-INTERVAL '10 days', NOW(), 'bbbbbbbb-0002-0000-0000-000000000002', 'COMPLETED', 'CSV',       'sales_may_week3.csv',      9800,  9800,   0,    NOW()-INTERVAL '10 days', NOW()-INTERVAL '10 days'+INTERVAL '1 min 22 sec', NULL, 'analyst'),
('cccccccc-0007-0000-0000-000000000007', NOW()-INTERVAL '52 days', NOW(), 'bbbbbbbb-0003-0000-0000-000000000003', 'COMPLETED', 'REST_API',  NULL,                       8900,  8900,   0,    NOW()-INTERVAL '52 days', NOW()-INTERVAL '52 days'+INTERVAL '58 sec',        NULL, 'SCHEDULER'),
('cccccccc-0008-0000-0000-000000000008', NOW()-INTERVAL '22 days', NOW(), 'bbbbbbbb-0003-0000-0000-000000000003', 'COMPLETED', 'REST_API',  NULL,                       9200,  9200,   0,    NOW()-INTERVAL '22 days', NOW()-INTERVAL '22 days'+INTERVAL '1 min 3 sec',   NULL, 'SCHEDULER'),
('cccccccc-0009-0000-0000-000000000009', NOW()-INTERVAL '48 days', NOW(), 'bbbbbbbb-0004-0000-0000-000000000004', 'COMPLETED', 'SCHEDULED', NULL,                       3200,  3200,   0,    NOW()-INTERVAL '48 days', NOW()-INTERVAL '48 days'+INTERVAL '28 sec',        NULL, 'SCHEDULER'),
('cccccccc-0010-0000-0000-000000000010', NOW()-INTERVAL '18 days', NOW(), 'bbbbbbbb-0004-0000-0000-000000000004', 'COMPLETED', 'SCHEDULED', NULL,                       3350,  3350,   0,    NOW()-INTERVAL '18 days', NOW()-INTERVAL '18 days'+INTERVAL '31 sec',        NULL, 'SCHEDULER'),
('cccccccc-0011-0000-0000-000000000011', NOW()-INTERVAL '39 days', NOW(), 'bbbbbbbb-0006-0000-0000-000000000006', 'COMPLETED', 'CSV',       'products_may2026.csv',     22400, 22400,  0,    NOW()-INTERVAL '39 days', NOW()-INTERVAL '39 days'+INTERVAL '2 min 55 sec', NULL, 'analyst'),
('cccccccc-0012-0000-0000-000000000012', NOW()-INTERVAL '5 days',  NOW(), 'bbbbbbbb-0006-0000-0000-000000000006', 'FAILED',    'CSV',       'products_corrupt.csv',     300,   0,      300,  NOW()-INTERVAL '5 days',  NOW()-INTERVAL '5 days' +INTERVAL '4 sec',         'File is corrupt or not valid CSV: unexpected EOF at row 12', 'analyst'),
('cccccccc-0013-0000-0000-000000000013', NOW()-INTERVAL '35 days', NOW(), 'bbbbbbbb-0007-0000-0000-000000000007', 'COMPLETED', 'REST_API',  NULL,                       6700,  6650,   50,   NOW()-INTERVAL '35 days', NOW()-INTERVAL '35 days'+INTERVAL '1 min 44 sec', NULL, 'SCHEDULER'),
('cccccccc-0014-0000-0000-000000000014', NOW()-INTERVAL '7 days',  NOW(), 'bbbbbbbb-0007-0000-0000-000000000007', 'COMPLETED', 'REST_API',  NULL,                       7100,  7100,   0,    NOW()-INTERVAL '7 days',  NOW()-INTERVAL '7 days' +INTERVAL '1 min 58 sec', NULL, 'SCHEDULER'),
('cccccccc-0015-0000-0000-000000000015', NOW()-INTERVAL '25 days', NOW(), 'bbbbbbbb-0009-0000-0000-000000000009', 'PARTIAL',   'REST_API',  NULL,                       1500,  1420,   80,   NOW()-INTERVAL '25 days', NOW()-INTERVAL '25 days'+INTERVAL '52 sec',        NULL, 'SCHEDULER'),
('cccccccc-0016-0000-0000-000000000016', NOW()-INTERVAL '3 min',   NOW(), 'bbbbbbbb-0001-0000-0000-000000000001', 'RUNNING',   'SCHEDULED', NULL,                       0,     0,      0,    NOW()-INTERVAL '3 min',   NULL,                                              NULL, 'SCHEDULER');

-- ─────────────────────────────────────────────────────────────
-- 5.  INGESTION RECORDS
-- Confirmed columns: id, created_at, updated_at,
--   job_id, data_source_id, raw_data, transformed_data,
--   status, error_message, column_name, source_row_number,
--   is_synchronized (NOT NULL), sync_job_id
-- ─────────────────────────────────────────────────────────────
INSERT INTO ingestion_records (id, created_at, updated_at, job_id, data_source_id, raw_data, transformed_data, status, error_message, column_name, source_row_number, is_synchronized, sync_job_id) VALUES
(gen_random_uuid(), NOW()-INTERVAL '10 days', NOW(), 'cccccccc-0006-0000-0000-000000000006', 'bbbbbbbb-0002-0000-0000-000000000002',
 '{"product_code":"SKU-001","unit_price":"29.99","region":"US","sale_date":"15/05/2026"}',
 '{"product_code":"sku-001","price":"29.99","region":"US","sale_date_iso":"2026-05-15"}',
 'PROCESSED', NULL, NULL, 1, true, 'dddddddd-0001-0000-0000-000000000001'),

(gen_random_uuid(), NOW()-INTERVAL '10 days', NOW(), 'cccccccc-0006-0000-0000-000000000006', 'bbbbbbbb-0002-0000-0000-000000000002',
 '{"product_code":"SKU-002","unit_price":"49.99","region":"EU","sale_date":"15/05/2026"}',
 '{"product_code":"sku-002","price":"49.99","region":"EU","sale_date_iso":"2026-05-15"}',
 'PROCESSED', NULL, NULL, 2, true, 'dddddddd-0001-0000-0000-000000000001'),

(gen_random_uuid(), NOW()-INTERVAL '10 days', NOW(), 'cccccccc-0006-0000-0000-000000000006', 'bbbbbbbb-0002-0000-0000-000000000002',
 '{"product_code":"SKU-003","unit_price":"9.99","region":null,"sale_date":"16/05/2026"}',
 '{"product_code":"sku-003","price":"9.99","region":"UNKNOWN","sale_date_iso":"2026-05-16"}',
 'PROCESSED', NULL, NULL, 3, true, 'dddddddd-0001-0000-0000-000000000001'),

(gen_random_uuid(), NOW()-INTERVAL '10 days', NOW(), 'cccccccc-0006-0000-0000-000000000006', 'bbbbbbbb-0002-0000-0000-000000000002',
 '{"product_code":"SKU-004","unit_price":"N/A","region":"APAC","sale_date":"16/05/2026"}',
 NULL, 'FAILED', 'Invalid numeric value "N/A" in field unit_price', 'unit_price', 4, false, NULL),

(gen_random_uuid(), NOW()-INTERVAL '7 days', NOW(), 'cccccccc-0014-0000-0000-000000000014', 'bbbbbbbb-0007-0000-0000-000000000007',
 '{"invoiceRef":"INV-1001","currency":"usd","amount":"1500.00"}',
 '{"invoice_ref":"INV-1001","currency":"USD","amount":"1500.00"}',
 'PROCESSED', NULL, NULL, 1, true, 'dddddddd-0003-0000-0000-000000000003'),

(gen_random_uuid(), NOW()-INTERVAL '7 days', NOW(), 'cccccccc-0014-0000-0000-000000000014', 'bbbbbbbb-0007-0000-0000-000000000007',
 '{"invoiceRef":"INV-1002","currency":"eur","amount":"890.50"}',
 '{"invoice_ref":"INV-1002","currency":"EUR","amount":"890.50"}',
 'PROCESSED', NULL, NULL, 2, true, 'dddddddd-0003-0000-0000-000000000003'),

(gen_random_uuid(), NOW()-INTERVAL '18 days', NOW(), 'cccccccc-0010-0000-0000-000000000010', 'bbbbbbbb-0004-0000-0000-000000000004',
 '{"dept_code":"eng","job_title":" Software Engineer ","employee_id":"E001"}',
 '{"dept_code":"ENG","job_title":"Software Engineer","employee_id":"E001"}',
 'PROCESSED', NULL, NULL, 1, true, 'dddddddd-0004-0000-0000-000000000004'),

(gen_random_uuid(), NOW()-INTERVAL '18 days', NOW(), 'cccccccc-0010-0000-0000-000000000010', 'bbbbbbbb-0004-0000-0000-000000000004',
 '{"dept_code":"hr","job_title":"HR Manager","employee_id":"E002"}',
 '{"dept_code":"HR","job_title":"HR Manager","employee_id":"E002"}',
 'PROCESSED', NULL, NULL, 2, true, 'dddddddd-0004-0000-0000-000000000004'),

(gen_random_uuid(), NOW()-INTERVAL '18 days', NOW(), 'cccccccc-0010-0000-0000-000000000010', 'bbbbbbbb-0004-0000-0000-000000000004',
 '{"dept_code":"fin","job_title":"Finance Analyst","employee_id":"E003"}',
 '{"dept_code":"FIN","job_title":"Finance Analyst","employee_id":"E003"}',
 'PROCESSED', NULL, NULL, 3, false, NULL),

(gen_random_uuid(), NOW()-INTERVAL '22 days', NOW(), 'cccccccc-0008-0000-0000-000000000008', 'bbbbbbbb-0003-0000-0000-000000000003',
 '{"campaignId":"C-2001","channel":"email","impressions":"45000","clicks":"1200"}',
 '{"campaign_id":"C-2001","channel":"EMAIL","impressions":"45000","clicks":"1200"}',
 'PROCESSED', NULL, NULL, 1, false, NULL),

(gen_random_uuid(), NOW()-INTERVAL '22 days', NOW(), 'cccccccc-0008-0000-0000-000000000008', 'bbbbbbbb-0003-0000-0000-000000000003',
 '{"campaignId":"C-2002","channel":"social","impressions":"120000","clicks":"3400"}',
 '{"campaign_id":"C-2002","channel":"SOCIAL","impressions":"120000","clicks":"3400"}',
 'PROCESSED', NULL, NULL, 2, false, NULL);

-- ─────────────────────────────────────────────────────────────
-- 6.  SYNC JOBS
-- Confirmed columns: id, created_at, updated_at,
--   data_source_id, status,
--   records_processed, records_failed, records_skipped, total_records,
--   started_at, completed_at, execution_time_ms,
--   error_message, triggered_by,
--   validation_passed, validation_failed
-- NOTE: start_time and end_time are STALE — drop them first:
--   ALTER TABLE sync_jobs DROP COLUMN IF EXISTS start_time;
--   ALTER TABLE sync_jobs DROP COLUMN IF EXISTS end_time;
-- ─────────────────────────────────────────────────────────────
INSERT INTO sync_jobs (id, created_at, updated_at, data_source_id, status, records_processed, records_failed, records_skipped, total_records, started_at, completed_at, execution_time_ms, error_message, triggered_by, validation_passed, validation_failed) VALUES
('dddddddd-0001-0000-0000-000000000001', NOW()-INTERVAL '10 days', NOW(), 'bbbbbbbb-0002-0000-0000-000000000002', 'COMPLETED', 9800,  0,  0, 9800,  NOW()-INTERVAL '10 days', NOW()-INTERVAL '10 days'+INTERVAL '12 sec', 12340, NULL,                                                          'SCHEDULER', 9800,  0),
('dddddddd-0002-0000-0000-000000000002', NOW()-INTERVAL '57 days', NOW(), 'bbbbbbbb-0001-0000-0000-000000000001', 'COMPLETED', 45200, 0,  0, 45200, NOW()-INTERVAL '57 days', NOW()-INTERVAL '57 days'+INTERVAL '38 sec', 38400, NULL,                                                          'SCHEDULER', 45200, 0),
('dddddddd-0003-0000-0000-000000000003', NOW()-INTERVAL '7 days',  NOW(), 'bbbbbbbb-0007-0000-0000-000000000007', 'COMPLETED', 7100,  0,  0, 7100,  NOW()-INTERVAL '7 days',  NOW()-INTERVAL '7 days' +INTERVAL '9 sec',  9100,  NULL,                                                          'SCHEDULER', 7100,  0),
('dddddddd-0004-0000-0000-000000000004', NOW()-INTERVAL '18 days', NOW(), 'bbbbbbbb-0004-0000-0000-000000000004', 'COMPLETED', 3350,  0,  0, 3350,  NOW()-INTERVAL '18 days', NOW()-INTERVAL '18 days'+INTERVAL '4 sec',  4200,  NULL,                                                          'SCHEDULER', 3350,  0),
('dddddddd-0005-0000-0000-000000000005', NOW()-INTERVAL '52 days', NOW(), 'bbbbbbbb-0003-0000-0000-000000000003', 'COMPLETED', 8900,  0,  0, 8900,  NOW()-INTERVAL '52 days', NOW()-INTERVAL '52 days'+INTERVAL '8 sec',  8800,  NULL,                                                          'SCHEDULER', 8900,  0),
('dddddddd-0006-0000-0000-000000000006', NOW()-INTERVAL '25 days', NOW(), 'bbbbbbbb-0009-0000-0000-000000000009', 'COMPLETED', 1420,  80, 0, 1500,  NOW()-INTERVAL '25 days', NOW()-INTERVAL '25 days'+INTERVAL '6 sec',  6100,  NULL,                                                          'SCHEDULER', 1420,  80),
('dddddddd-0007-0000-0000-000000000007', NOW()-INTERVAL '30 days', NOW(), 'bbbbbbbb-0008-0000-0000-000000000008', 'FAILED',    0,     0,  0, 0,     NOW()-INTERVAL '30 days', NOW()-INTERVAL '30 days'+INTERVAL '2 sec',  2100,  'Connection timeout: could not reach host wh-db.internal after 30s', 'SCHEDULER', 0, 0),
('dddddddd-0008-0000-0000-000000000008', NOW()-INTERVAL '39 days', NOW(), 'bbbbbbbb-0006-0000-0000-000000000006', 'COMPLETED', 22400, 0,  0, 22400, NOW()-INTERVAL '39 days', NOW()-INTERVAL '39 days'+INTERVAL '18 sec', 18200, NULL,                                                          'analyst',   22400, 0),
('dddddddd-0009-0000-0000-000000000009', NOW()-INTERVAL '43 days', NOW(), 'bbbbbbbb-0001-0000-0000-000000000001', 'COMPLETED', 46800, 0,  0, 46800, NOW()-INTERVAL '43 days', NOW()-INTERVAL '43 days'+INTERVAL '41 sec', 41200, NULL,                                                          'SCHEDULER', 46800, 0),
('dddddddd-0010-0000-0000-000000000010', NOW()-INTERVAL '54 days', NOW(), 'bbbbbbbb-0002-0000-0000-000000000002', 'COMPLETED', 12750, 50, 0, 12800, NOW()-INTERVAL '54 days', NOW()-INTERVAL '54 days'+INTERVAL '11 sec', 11200, NULL,                                                          'SCHEDULER', 12750, 50),
('dddddddd-0011-0000-0000-000000000011', NOW()-INTERVAL '35 days', NOW(), 'bbbbbbbb-0007-0000-0000-000000000007', 'COMPLETED', 6650,  50, 0, 6700,  NOW()-INTERVAL '35 days', NOW()-INTERVAL '35 days'+INTERVAL '7 sec',  7300,  NULL,                                                          'SCHEDULER', 6650,  50),
('dddddddd-0012-0000-0000-000000000012', NOW()-INTERVAL '15 days', NOW(), 'bbbbbbbb-0001-0000-0000-000000000001', 'COMPLETED', 47500, 0,  0, 47500, NOW()-INTERVAL '15 days', NOW()-INTERVAL '15 days'+INTERVAL '44 sec', 44100, NULL,                                                          'SCHEDULER', 47500, 0);

-- ─────────────────────────────────────────────────────────────
-- 7.  AUDIT LOGS
-- Confirmed columns: id, action, username, status,
--   details, ip_address, timestamp
-- (No created_at/updated_at — AuditEntity has its own @Id,
--  does NOT extend BaseEntity)
-- ─────────────────────────────────────────────────────────────
INSERT INTO audit_logs (id, action, username, status, details, ip_address, timestamp) VALUES
(gen_random_uuid(),'USER_REGISTER',          'admin',    'SUCCESS','New user registered: admin',                                                     '192.168.1.10', NOW()-INTERVAL '60 days'),
(gen_random_uuid(),'USER_LOGIN',             'admin',    'SUCCESS','Login successful',                                                               '192.168.1.10', NOW()-INTERVAL '60 days'),
(gen_random_uuid(),'USER_REGISTER',          'analyst',  'SUCCESS','New user registered: analyst',                                                   '192.168.1.22', NOW()-INTERVAL '55 days'),
(gen_random_uuid(),'USER_LOGIN',             'analyst',  'SUCCESS','Login successful',                                                               '192.168.1.22', NOW()-INTERVAL '55 days'),
(gen_random_uuid(),'USER_REGISTER',          'operator', 'SUCCESS','New user registered: operator',                                                  '192.168.1.35', NOW()-INTERVAL '50 days'),
(gen_random_uuid(),'CREATE_USER',            'admin',    'SUCCESS','Created user: username=alice, role=ANALYST',                                     '192.168.1.10', NOW()-INTERVAL '45 days'),
(gen_random_uuid(),'CREATE_USER',            'admin',    'SUCCESS','Created user: username=bob, role=OPERATOR',                                      '192.168.1.10', NOW()-INTERVAL '40 days'),
(gen_random_uuid(),'CREATE_DATA_SOURCE',     'admin',    'SUCCESS','Created data source: name=''CRM PostgreSQL'', type=DATABASE',                    '192.168.1.10', NOW()-INTERVAL '58 days'),
(gen_random_uuid(),'CREATE_DATA_SOURCE',     'admin',    'SUCCESS','Created data source: name=''Sales CSV Export'', type=CSV',                       '192.168.1.10', NOW()-INTERVAL '55 days'),
(gen_random_uuid(),'CREATE_DATA_SOURCE',     'analyst',  'SUCCESS','Created data source: name=''Marketing REST API'', type=REST_API',                '192.168.1.22', NOW()-INTERVAL '52 days'),
(gen_random_uuid(),'UPDATE_DATA_SOURCE',     'admin',    'SUCCESS','Updated data source: name=''Warehouse Database'', status=ERROR',                 '192.168.1.10', NOW()-INTERVAL '30 days'),
(gen_random_uuid(),'CREATE_RULE',            'admin',    'SUCCESS','Created rule: name=''Uppercase Email'', type=UPPERCASE',                         '192.168.1.10', NOW()-INTERVAL '57 days'),
(gen_random_uuid(),'CREATE_RULE',            'analyst',  'SUCCESS','Created rule: name=''Full Name Concat'', type=CONCAT',                          '192.168.1.22', NOW()-INTERVAL '56 days'),
(gen_random_uuid(),'CREATE_RULE',            'analyst',  'SUCCESS','Created rule: name=''Normalise Sale Date'', type=DATE_FORMAT',                   '192.168.1.22', NOW()-INTERVAL '54 days'),
(gen_random_uuid(),'UPLOAD_CSV',             'analyst',  'SUCCESS','CSV uploaded: sales_q1_2026.csv, dataSourceId=bbbbbbbb-0002-...',                '192.168.1.22', NOW()-INTERVAL '54 days'),
(gen_random_uuid(),'INGESTION_STARTED',      'analyst',  'SUCCESS','Ingestion started: jobId=cccccccc-0004-..., file=sales_q1_2026.csv',             '192.168.1.22', NOW()-INTERVAL '54 days'),
(gen_random_uuid(),'INGESTION_COMPLETED',    'analyst',  'SUCCESS','Ingestion completed: jobId=cccccccc-0004-..., processed=12750, failed=50',       '192.168.1.22', NOW()-INTERVAL '54 days'),
(gen_random_uuid(),'TRANSFORMATION_EXECUTED','analyst',  'SUCCESS','Applied 3 rules to jobId=cccccccc-0004-...: transformed=12750',                  '192.168.1.22', NOW()-INTERVAL '53 days'),
(gen_random_uuid(),'UPLOAD_CSV',             'analyst',  'SUCCESS','CSV uploaded: sales_q2_bad_format.csv',                                         '192.168.1.22', NOW()-INTERVAL '40 days'),
(gen_random_uuid(),'INGESTION_FAILED',       'analyst',  'FAILED', 'Ingestion failed: jobId=cccccccc-0005-..., error=Invalid column format at row 42','192.168.1.22',NOW()-INTERVAL '40 days'),
(gen_random_uuid(),'SYNC_STARTED',           'SCHEDULER','RUNNING','Sync started: jobId=dddddddd-0002-..., dataSource=''CRM PostgreSQL''',           '127.0.0.1',    NOW()-INTERVAL '57 days'),
(gen_random_uuid(),'SYNC_COMPLETED',         'SCHEDULER','COMPLETED','Sync completed: jobId=dddddddd-0002-..., processed=45200, ms=38400',           '127.0.0.1',    NOW()-INTERVAL '57 days'),
(gen_random_uuid(),'SYNC_STARTED',           'SCHEDULER','RUNNING','Sync started: jobId=dddddddd-0007-..., dataSource=''Warehouse Database''',       '127.0.0.1',    NOW()-INTERVAL '30 days'),
(gen_random_uuid(),'SYNC_FAILED',            'SCHEDULER','FAILED', 'Sync failed: jobId=dddddddd-0007-..., error=Connection timeout after 30s',       '127.0.0.1',    NOW()-INTERVAL '30 days'),
(gen_random_uuid(),'SYNC_STARTED',           'SCHEDULER','RUNNING','Sync started: jobId=dddddddd-0001-..., dataSource=''Sales CSV Export''',         '127.0.0.1',    NOW()-INTERVAL '10 days'),
(gen_random_uuid(),'SYNC_COMPLETED',         'SCHEDULER','COMPLETED','Sync completed: jobId=dddddddd-0001-..., processed=9800, ms=12340',            '127.0.0.1',    NOW()-INTERVAL '10 days'),
(gen_random_uuid(),'SYNC_STARTED',           'SCHEDULER','RUNNING','Sync started: jobId=dddddddd-0003-..., dataSource=''Finance REST API''',         '127.0.0.1',    NOW()-INTERVAL '7 days'),
(gen_random_uuid(),'SYNC_COMPLETED',         'SCHEDULER','COMPLETED','Sync completed: jobId=dddddddd-0003-..., processed=7100, ms=9100',             '127.0.0.1',    NOW()-INTERVAL '7 days'),
(gen_random_uuid(),'USER_LOGIN',             'analyst',  'FAILED', 'Login failed: bad credentials',                                                  '10.0.0.99',    NOW()-INTERVAL '3 days'),
(gen_random_uuid(),'UPLOAD_CSV',             'analyst',  'SUCCESS','CSV uploaded: products_corrupt.csv',                                             '192.168.1.22', NOW()-INTERVAL '5 days'),
(gen_random_uuid(),'INGESTION_FAILED',       'analyst',  'FAILED', 'Ingestion failed: jobId=cccccccc-0012-..., error=Corrupt CSV unexpected EOF',    '192.168.1.22', NOW()-INTERVAL '5 days'),
(gen_random_uuid(),'USER_LOGIN',             'alice',    'SUCCESS','Login successful',                                                               '192.168.1.44', NOW()-INTERVAL '2 days'),
(gen_random_uuid(),'USER_LOGIN',             'bob',      'SUCCESS','Login successful',                                                               '192.168.1.55', NOW()-INTERVAL '1 day'),
(gen_random_uuid(),'USER_LOGIN',             'admin',    'SUCCESS','Login successful',                                                               '192.168.1.10', NOW()-INTERVAL '2 hours'),
(gen_random_uuid(),'USER_LOGOUT',            'operator', 'SUCCESS','User logged out',                                                                '192.168.1.35', NOW()-INTERVAL '1 hour');
