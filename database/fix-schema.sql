-- =============================================================
-- EDIP — Complete Schema Fix
-- Drops ALL stale columns from old schema.sql that conflict
-- with the current Hibernate entities.
-- Run this ONCE in Supabase SQL Editor, then run seed-data.sql
-- =============================================================

-- ── transformation_rules ─────────────────────────────────────
-- Old schema.sql columns not in entity: enabled, rule_type, config
-- Entity columns: active, transformation_type, source_field,
--   target_field, default_value, extra_config, execution_order,
--   created_by, data_source_id, name, description

ALTER TABLE transformation_rules DROP COLUMN IF EXISTS enabled;
ALTER TABLE transformation_rules DROP COLUMN IF EXISTS rule_type;
ALTER TABLE transformation_rules DROP COLUMN IF EXISTS config;

-- ── data_sources ─────────────────────────────────────────────
-- Old schema.sql columns not in entity: type, config, created_by (was UUID ref)
-- Entity columns: source_type, connection_details, status,
--   description, created_by (VARCHAR)

ALTER TABLE data_sources DROP COLUMN IF EXISTS type;
ALTER TABLE data_sources DROP COLUMN IF EXISTS config;

-- ── sync_jobs ─────────────────────────────────────────────────
-- Old schema.sql columns not in entity: source_id, target_id,
--   schedule, last_run_at
-- Entity columns: data_source_id, status, records_processed,
--   records_failed, records_skipped, total_records, started_at,
--   completed_at, execution_time_ms, error_message, triggered_by,
--   validation_passed, validation_failed

ALTER TABLE sync_jobs DROP COLUMN IF EXISTS source_id;
ALTER TABLE sync_jobs DROP COLUMN IF EXISTS target_id;
ALTER TABLE sync_jobs DROP COLUMN IF EXISTS schedule;
ALTER TABLE sync_jobs DROP COLUMN IF EXISTS last_run_at;

-- ── audit_logs ────────────────────────────────────────────────
-- Old schema.sql columns not in entity: user_id, entity_type,
--   entity_id, details (was JSONB — entity has VARCHAR)
-- Entity columns: action, username, status, details (VARCHAR),
--   ip_address, timestamp

ALTER TABLE audit_logs DROP COLUMN IF EXISTS user_id;
ALTER TABLE audit_logs DROP COLUMN IF EXISTS entity_type;
ALTER TABLE audit_logs DROP COLUMN IF EXISTS entity_id;

-- ── ingestion_jobs ────────────────────────────────────────────
-- Old schema.sql had no extra stale columns beyond what Hibernate manages
-- Nothing to drop here

-- ── users ─────────────────────────────────────────────────────
-- Old schema.sql had no extra stale columns
-- Nothing to drop here

-- =============================================================
-- VERIFY: run this SELECT to see all remaining columns
-- Uncomment to check before running seed-data.sql
-- =============================================================
/*
SELECT
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN (
      'users','data_sources','transformation_rules',
      'ingestion_jobs','ingestion_records',
      'sync_jobs','audit_logs','refresh_tokens'
  )
ORDER BY table_name, ordinal_position;
*/
