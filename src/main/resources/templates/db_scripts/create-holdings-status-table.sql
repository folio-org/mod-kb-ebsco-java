ALTER TABLE holdings_status
-- Add unique column with value true to ensure that only one row can be created in holdings_status
  ADD COLUMN IF NOT EXISTS lock BOOL NOT NULL UNIQUE DEFAULT TRUE CHECK (lock);

ALTER TABLE holdings_status
  ADD COLUMN IF NOT EXISTS process_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

ALTER TABLE holdings_status
  ALTER COLUMN process_id DROP DEFAULT;

ALTER TABLE holdings_status
  ADD COLUMN IF NOT EXISTS credentials_id UUID NOT NULL;

ALTER TABLE holdings_status
  DROP CONSTRAINT IF EXISTS holdings_status_lock_key,
  DROP CONSTRAINT IF EXISTS unq_holdings_status_kb_credentialsid_lock,
  ADD CONSTRAINT unq_holdings_status_kb_credentialsid_lock UNIQUE (credentials_id, lock);

ALTER TABLE holdings_status
  DROP CONSTRAINT IF EXISTS fk_holdings_status_kb_credentials,
  ADD CONSTRAINT fk_holdings_status_kb_credentials FOREIGN KEY (credentials_id) REFERENCES kb_credentials (id) ON DELETE CASCADE;
