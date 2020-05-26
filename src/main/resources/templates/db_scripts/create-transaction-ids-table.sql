ALTER TABLE transaction_ids
  DROP COLUMN IF EXISTS jsonb,
  DROP CONSTRAINT IF EXISTS transaction_ids_pkey,
  DROP COLUMN IF EXISTS id,
  ADD COLUMN IF NOT EXISTS credentials_id UUID NOT NULL,
  ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(50),
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE transaction_ids
  DROP CONSTRAINT IF EXISTS fk_transaction_ids_kb_credentials,
  ADD CONSTRAINT fk_transaction_ids_kb_credentials FOREIGN KEY (credentials_id) REFERENCES kb_credentials (id) ON DELETE CASCADE;
