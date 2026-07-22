-- =============================================================
-- EDIP — Docker Schema Fix
-- Transforms the tables created by schema.sql into the full
-- entity-aligned schema expected by seed-data.sql.
-- Run this ONCE after schema.sql, then run seed-data.sql.
-- =============================================================

-- ─────────────────────────────────────────────────────────────
-- 1. USERS — add first_name, last_name
-- ─────────────────────────────────────────────────────────────
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name  VARCHAR(100);

-- ─────────────────────────────────────────────────────────────
-- 2. DATA SOURCES — rename type→source_type, config→connection_details,
--    fix created_by from UUID ref to VARCHAR, add description
-- ─────────────────────────────────────────────────────────────
ALTER TABLE data_sources DROP COLUMN IF EXISTS type;
ALTER TABLE data_sources DROP COLUMN IF EXISTS config;
ALTER TABLE data_sources DROP COLUMN IF EXISTS created_by;

ALTER TABLE data_sources ADD COLUMN IF NOT EXISTS source_type        VARCHAR(100);
ALTER TABLE data_sources ADD COLUMN IF NOT EXISTS connection_details JSONB;
ALTER TABLE data_sources ADD COLUMN IF NOT EXISTS description        TEXT;
ALTER TABLE data_sources ADD COLUMN IF NOT EXISTS created_by         VARCHAR(100);

-- ─────────────────────────────────────────────────────────────
-- 3. TRANSFORMATION RULES — drop old columns, add entity columns
-- ─────────────────────────────────────────────────────────────
ALTER TABLE transformation_rules DROP COLUMN IF EXISTS enabled;
ALTER TABLE transformation_rules DROP COLUMN IF EXISTS rule_type;
ALTER TABLE transformation_rules DROP COLUMN IF EXISTS config;
ALTER TABLE transformation_rules DROP COLUMN IF EXISTS created_by;

ALTER TABLE transformation_rules ADD COLUMN IF NOT EXISTS description         TEXT;
ALTER TABLE transformation_rules ADD COLUMN IF NOT EXISTS data_source_id      UUID REFERENCES data_sources(id);
ALTER TABLE transformation_rules ADD COLUMN IF NOT EXISTS transformation_type VARCHAR(100);
ALTER TABLE transformation_rules ADD COLUMN IF NOT EXISTS source_field        VARCHAR(255);
ALTER TABLE transformation_rules ADD COLUMN IF NOT EXISTS target_field        VARCHAR(255);
ALTER TABLE transformation_rules ADD COLUMN IF NOT EXISTS default_value       VARCHAR(255);
ALTER TABLE transformation_rules ADD COLUMN IF NOT EXISTS extra_config        VARCHAR(255);
ALTER TABLE transformation_rules ADD COLUMN IF NOT EXISTS active              BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE transformation_rules ADD COLUMN IF NOT EXISTS execution_order     INTEGER;
ALTER TABLE transformation_rules ADD COLUMN IF NOT EXISTS created_by          VARCHAR(100);

-- ─────────────────────────────────────────────────────────────
-- 4. INGESTION JOBS — add missing columns
-- ─────────────────────────────────────────────────────────────
ALTER TABLE ingestion_jobs ADD COLUMN IF NOT EXISTS updated_at         TIMESTAMP;
ALTER TABLE ingestion_jobs ADD COLUMN IF NOT EXISTS ingestion_type     VARCHAR(100);
ALTER TABLE ingestion_jobs ADD COLUMN IF NOT EXISTS file_name          VARCHAR(500);
ALTER TABLE ingestion_jobs ADD COLUMN IF NOT EXISTS total_records      BIGINT DEFAULT 0;
ALTER TABLE ingestion_jobs ADD COLUMN IF NOT EXISTS records_processed  BIGINT DEFAULT 0;
ALTER TABLE ingestion_jobs ADD COLUMN IF NOT EXISTS records_failed     BIGINT DEFAULT 0;
ALTER TABLE ingestion_jobs ADD COLUMN IF NOT EXISTS triggered_by       VARCHAR(100);

-- ─────────────────────────────────────────────────────────────
-- 5. INGESTION RECORDS — create missing table
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ingestion_records (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP,
    job_id            UUID        REFERENCES ingestion_jobs(id),
    data_source_id    UUID        REFERENCES data_sources(id),
    raw_data          TEXT,
    transformed_data  TEXT,
    status            VARCHAR(50),
    error_message     TEXT,
    column_name       VARCHAR(255),
    source_row_number INTEGER,
    is_synchronized   BOOLEAN     NOT NULL DEFAULT FALSE,
    sync_job_id       UUID
);

-- ─────────────────────────────────────────────────────────────
-- 6. SYNC JOBS — drop stale columns, add entity columns
-- ─────────────────────────────────────────────────────────────
ALTER TABLE sync_jobs DROP COLUMN IF EXISTS source_id;
ALTER TABLE sync_jobs DROP COLUMN IF EXISTS target_id;
ALTER TABLE sync_jobs DROP COLUMN IF EXISTS schedule;
ALTER TABLE sync_jobs DROP COLUMN IF EXISTS last_run_at;

ALTER TABLE sync_jobs ADD COLUMN IF NOT EXISTS updated_at          TIMESTAMP;
ALTER TABLE sync_jobs ADD COLUMN IF NOT EXISTS data_source_id      UUID REFERENCES data_sources(id);
ALTER TABLE sync_jobs ADD COLUMN IF NOT EXISTS records_processed   BIGINT DEFAULT 0;
ALTER TABLE sync_jobs ADD COLUMN IF NOT EXISTS records_failed      BIGINT DEFAULT 0;
ALTER TABLE sync_jobs ADD COLUMN IF NOT EXISTS records_skipped     BIGINT DEFAULT 0;
ALTER TABLE sync_jobs ADD COLUMN IF NOT EXISTS total_records       BIGINT DEFAULT 0;
ALTER TABLE sync_jobs ADD COLUMN IF NOT EXISTS started_at          TIMESTAMP;
ALTER TABLE sync_jobs ADD COLUMN IF NOT EXISTS completed_at        TIMESTAMP;
ALTER TABLE sync_jobs ADD COLUMN IF NOT EXISTS execution_time_ms   BIGINT;
ALTER TABLE sync_jobs ADD COLUMN IF NOT EXISTS error_message       TEXT;
ALTER TABLE sync_jobs ADD COLUMN IF NOT EXISTS triggered_by        VARCHAR(100);
ALTER TABLE sync_jobs ADD COLUMN IF NOT EXISTS validation_passed   BIGINT DEFAULT 0;
ALTER TABLE sync_jobs ADD COLUMN IF NOT EXISTS validation_failed   BIGINT DEFAULT 0;

-- ─────────────────────────────────────────────────────────────
-- 7. AUDIT LOGS — drop UUID-based columns, add entity columns
-- ─────────────────────────────────────────────────────────────
ALTER TABLE audit_logs DROP COLUMN IF EXISTS user_id;
ALTER TABLE audit_logs DROP COLUMN IF EXISTS entity_type;
ALTER TABLE audit_logs DROP COLUMN IF EXISTS entity_id;
ALTER TABLE audit_logs DROP COLUMN IF EXISTS details;
ALTER TABLE audit_logs DROP COLUMN IF EXISTS created_at;

ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS username   VARCHAR(255);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS status     VARCHAR(50);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS details    VARCHAR(1000);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS timestamp  TIMESTAMP DEFAULT NOW();
