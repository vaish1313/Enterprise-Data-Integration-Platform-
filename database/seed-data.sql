-- ============================================================
-- Enterprise Data Integration Platform - Seed Data
-- ============================================================

-- Default admin user (password: Admin@123 - bcrypt hashed)
INSERT INTO users (id, username, email, password, role, enabled)
VALUES (
  gen_random_uuid(),
  'admin',
  'admin@company.com',
  '$2a$12$placeholder_bcrypt_hash_here',
  'ADMIN',
  TRUE
);

-- Sample data source
INSERT INTO data_sources (name, type, config, status)
VALUES (
  'Sample PostgreSQL Source',
  'POSTGRESQL',
  '{"host": "localhost", "port": 5432, "database": "sample_db"}',
  'ACTIVE'
);
