-- Add retry_count and last_attempted_at to ingestion_jobs
ALTER TABLE ingestion_jobs
ADD COLUMN IF NOT EXISTS retry_count INTEGER DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS last_attempted_at TIMESTAMP;

-- Add retry_count and last_attempted_at to sync_jobs
ALTER TABLE sync_jobs
ADD COLUMN IF NOT EXISTS retry_count INTEGER DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS last_attempted_at TIMESTAMP;
