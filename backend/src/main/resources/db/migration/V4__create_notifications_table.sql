-- V4__create_notifications_table.sql

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL, -- e.g., 'SUCCESS', 'ERROR', 'WARNING', 'INFO'
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    related_entity_type VARCHAR(50), -- e.g., 'SYNC_JOB', 'INGESTION_JOB'
    related_entity_id UUID,
    
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Optimize for: "Get all notifications for user X, sorted by created_at" 
-- and "Get unread count for user X"
CREATE INDEX idx_notifications_user_read_time ON notifications(user_id, is_read, created_at DESC);
