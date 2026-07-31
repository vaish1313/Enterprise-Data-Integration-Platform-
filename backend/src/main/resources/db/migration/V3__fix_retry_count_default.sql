-- Fix retry_count default and nullability for ingestion_jobs
UPDATE ingestion_jobs SET retry_count = 0 WHERE retry_count IS NULL;
ALTER TABLE ingestion_jobs ALTER COLUMN retry_count SET DEFAULT 0;
ALTER TABLE ingestion_jobs ALTER COLUMN retry_count SET NOT NULL;

-- Fix retry_count default and nullability for sync_jobs
UPDATE sync_jobs SET retry_count = 0 WHERE retry_count IS NULL;
ALTER TABLE sync_jobs ALTER COLUMN retry_count SET DEFAULT 0;
ALTER TABLE sync_jobs ALTER COLUMN retry_count SET NOT NULL;
