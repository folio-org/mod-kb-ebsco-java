ALTER TABLE retry_status
  DROP COLUMN IF EXISTS jsonb,
  ADD COLUMN IF NOT EXISTS credentials_id UUID NOT NULL,
  ADD COLUMN IF NOT EXISTS attempts_left INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS timer_id BIGINT;

ALTER TABLE retry_status
  DROP CONSTRAINT IF EXISTS fk_retry_status_kb_credentials,
  ADD CONSTRAINT fk_retry_status_kb_credentials FOREIGN KEY (credentials_id) REFERENCES kb_credentials (id) ON DELETE CASCADE;
