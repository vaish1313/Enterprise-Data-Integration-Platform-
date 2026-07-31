-- =============================================================
-- V1__initial_schema.sql
-- Enterprise Data Integration Platform — Initial Schema
--
-- Creates all tables exactly as expected by the JPA entities.
-- This is the single source of truth for a fresh database clone.
-- V2 adds retry fields; V3 fixes their defaults.
-- =============================================================

-- ─────────────────────────────────────────────────────────────
-- 1. USERS
--    Entity: com.company.integrationplatform.user.entity.User
--    Extends: BaseEntity (id UUID, created_at, updated_at)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE users (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    username    VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    first_name  VARCHAR(100),
    last_name   VARCHAR(100),

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email)
);

-- ─────────────────────────────────────────────────────────────
-- 2. REFRESH TOKENS
--    Entity: com.company.integrationplatform.auth.entity.RefreshToken
--    Does NOT extend BaseEntity (manages created_at itself)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_refresh_token UNIQUE (token)
);

-- ─────────────────────────────────────────────────────────────
-- 3. DATA SOURCES
--    Entity: com.company.integrationplatform.datasource.DataSourceEntity
--    Extends: BaseEntity (id UUID, created_at, updated_at)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE data_sources (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    name               VARCHAR(255) NOT NULL,
    source_type        VARCHAR(50)  NOT NULL,
    connection_details JSONB,
    status             VARCHAR(50)  NOT NULL DEFAULT 'INACTIVE',
    description        VARCHAR(500),
    created_by         VARCHAR(100)
);

-- ─────────────────────────────────────────────────────────────
-- 4. TRANSFORMATION RULES
--    Entity: com.company.integrationplatform.transformation.TransformationRule
--    Extends: BaseEntity (id UUID, created_at, updated_at)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE transformation_rules (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         VARCHAR(500),
    data_source_id      UUID         REFERENCES data_sources(id),
    transformation_type VARCHAR(50)  NOT NULL,
    source_field        VARCHAR(255),
    target_field        VARCHAR(255),
    default_value       VARCHAR(500),
    extra_config        VARCHAR(500),
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    execution_order     INTEGER      NOT NULL DEFAULT 0,
    created_by          VARCHAR(100)
);

CREATE INDEX idx_transform_datasource ON transformation_rules (data_source_id);
CREATE INDEX idx_transform_active     ON transformation_rules (active);
CREATE INDEX idx_transform_order      ON transformation_rules (execution_order);

-- ─────────────────────────────────────────────────────────────
-- 5. INGESTION JOBS
--    Entity: com.company.integrationplatform.ingestion.IngestionJob
--    Extends: BaseEntity (id UUID, created_at, updated_at)
--    Note: retry_count and last_attempted_at are added in V2/V3.
-- ─────────────────────────────────────────────────────────────
CREATE TABLE ingestion_jobs (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    data_source_id    UUID         NOT NULL REFERENCES data_sources(id),
    status            VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    ingestion_type    VARCHAR(50)  NOT NULL,
    file_name         VARCHAR(500),
    total_records     BIGINT       NOT NULL DEFAULT 0,
    records_processed BIGINT       NOT NULL DEFAULT 0,
    records_failed    BIGINT       NOT NULL DEFAULT 0,
    started_at        TIMESTAMP,
    completed_at      TIMESTAMP,
    error_message     VARCHAR(1000),
    triggered_by      VARCHAR(100)
);

CREATE INDEX idx_ingest_job_datasource ON ingestion_jobs (data_source_id);
CREATE INDEX idx_ingest_job_status     ON ingestion_jobs (status);
CREATE INDEX idx_ingest_job_type       ON ingestion_jobs (ingestion_type);

-- ─────────────────────────────────────────────────────────────
-- 6. INGESTION RECORDS
--    Entity: com.company.integrationplatform.ingestion.IngestionRecord
--    Extends: BaseEntity (id UUID, created_at, updated_at)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE ingestion_records (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at        TIMESTAMP   NOT NULL,
    updated_at        TIMESTAMP   NOT NULL,
    job_id            UUID        NOT NULL REFERENCES ingestion_jobs(id),
    data_source_id    UUID        NOT NULL REFERENCES data_sources(id),
    raw_data          JSONB,
    transformed_data  JSONB,
    status            VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message     VARCHAR(1000),
    column_name       VARCHAR(255),
    source_row_number INTEGER,
    is_synchronized   BOOLEAN     NOT NULL DEFAULT FALSE,
    sync_job_id       UUID
);

CREATE INDEX idx_ingest_rec_job    ON ingestion_records (job_id);
CREATE INDEX idx_ingest_rec_status ON ingestion_records (status);

-- ─────────────────────────────────────────────────────────────
-- 7. SYNC JOBS
--    Entity: com.company.integrationplatform.synchronization.SyncJob
--    Extends: BaseEntity (id UUID, created_at, updated_at)
--    Note: retry_count and last_attempted_at are added in V2/V3.
-- ─────────────────────────────────────────────────────────────
CREATE TABLE sync_jobs (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    data_source_id    UUID         NOT NULL REFERENCES data_sources(id),
    status            VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    records_processed BIGINT       NOT NULL DEFAULT 0,
    records_failed    BIGINT       NOT NULL DEFAULT 0,
    records_skipped   BIGINT       NOT NULL DEFAULT 0,
    total_records     BIGINT       NOT NULL DEFAULT 0,
    started_at        TIMESTAMP,
    completed_at      TIMESTAMP,
    execution_time_ms BIGINT,
    error_message     VARCHAR(1000),
    triggered_by      VARCHAR(100) DEFAULT 'SCHEDULER',
    validation_passed BIGINT       NOT NULL DEFAULT 0,
    validation_failed BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_sync_job_datasource ON sync_jobs (data_source_id);
CREATE INDEX idx_sync_job_status     ON sync_jobs (status);
CREATE INDEX idx_sync_job_started    ON sync_jobs (started_at);

-- ─────────────────────────────────────────────────────────────
-- 8. AUDIT LOGS
--    Entity: com.company.integrationplatform.audit.AuditEntity
--    Does NOT extend BaseEntity (manages id and timestamp itself)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    action     VARCHAR(100)  NOT NULL,
    username   VARCHAR(100)  NOT NULL,
    status     VARCHAR(50)   NOT NULL,
    details    VARCHAR(1000),
    ip_address VARCHAR(50),
    timestamp  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user      ON audit_logs (username);
CREATE INDEX idx_audit_action    ON audit_logs (action);
CREATE INDEX idx_audit_timestamp ON audit_logs (timestamp);
