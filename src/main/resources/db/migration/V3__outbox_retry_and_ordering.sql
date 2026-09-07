ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP WITH TIME ZONE;

DROP INDEX IF EXISTS idx_outbox_pending;
CREATE INDEX IF NOT EXISTS idx_outbox_pending ON outbox_events (next_retry_at, created_at, id) WHERE status = 'PENDING';
