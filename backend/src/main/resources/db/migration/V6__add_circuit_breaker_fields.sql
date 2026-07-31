-- =============================================================
-- V6__add_circuit_breaker_fields.sql
-- Enterprise Data Integration Platform
--
-- Adds circuit breaker state machine columns to data_sources.
-- Operates at the DATA SOURCE level, tracking failures ACROSS
-- multiple separate job executions (not retries within one job).
--
-- Circuit states:
--   CLOSED    -- normal operation (default)
--   HALF_OPEN -- one cautious test attempt allowed after cooldown
--   OPEN      -- suspended; scheduler skips this source entirely
--
-- SourceStatus additions (in Java enum, not enforced by DB):
--   DEGRADED  -- maps to circuit_state = HALF_OPEN
--   SUSPENDED -- maps to circuit_state = OPEN
-- =============================================================

ALTER TABLE data_sources
    -- Counts permanently failed jobs (after all retries exhausted).
    -- Reset to 0 on any successful job. Triggers OPEN at 3.
    ADD COLUMN IF NOT EXISTS consecutive_failure_count INTEGER NOT NULL DEFAULT 0,

    -- The circuit breaker state machine state.
    -- Values: CLOSED (normal), HALF_OPEN (testing), OPEN (suspended).
    ADD COLUMN IF NOT EXISTS circuit_state VARCHAR(20) NOT NULL DEFAULT 'CLOSED',

    -- Timestamp of the last permanently failed job for this source.
    -- NULL when circuit is CLOSED and no failures have occurred.
    ADD COLUMN IF NOT EXISTS last_failure_at TIMESTAMP,

    -- Auto-recovery timestamp. When NOW() > suspended_until and
    -- circuit_state = OPEN, the system transitions to HALF_OPEN.
    -- NULL when circuit is CLOSED.
    ADD COLUMN IF NOT EXISTS suspended_until TIMESTAMP;

-- Index to efficiently find sources that are ready to transition
-- from OPEN -> HALF_OPEN (scheduler polls this on every tick).
CREATE INDEX IF NOT EXISTS idx_ds_circuit_state
    ON data_sources (circuit_state);

-- Index for the scheduler's auto-recovery window query:
-- WHERE circuit_state = 'OPEN' AND suspended_until <= NOW()
CREATE INDEX IF NOT EXISTS idx_ds_suspended_until
    ON data_sources (suspended_until)
    WHERE suspended_until IS NOT NULL;

-- Add CHECK constraint to keep circuit_state values clean.
ALTER TABLE data_sources
    ADD CONSTRAINT chk_circuit_state
        CHECK (circuit_state IN ('CLOSED', 'HALF_OPEN', 'OPEN'));

-- Ensure all existing rows are in a valid starting state.
-- (consecutive_failure_count and circuit_state already default correctly,
-- but this makes the intent explicit for any pre-existing ERROR rows.)
UPDATE data_sources
    SET circuit_state             = 'CLOSED',
        consecutive_failure_count = 0
    WHERE circuit_state IS NULL;
