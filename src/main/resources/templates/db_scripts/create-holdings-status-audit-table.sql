ALTER TABLE holdings_status_audit
  DROP COLUMN IF EXISTS id,
  ADD COLUMN IF NOT EXISTS credentials_id UUID NOT NULL,
  ADD COLUMN IF NOT EXISTS operation VARCHAR(20) NOT NULL,
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE holdings_status_audit
  DROP CONSTRAINT IF EXISTS fk_holdings_status_audit_kb_credentials,
  ADD CONSTRAINT fk_holdings_status_audit_kb_credentials FOREIGN KEY (credentials_id) REFERENCES kb_credentials (id);

CREATE INDEX IF NOT EXISTS idx_holdings_status_audit_kb_credentialsid ON holdings_status_audit (credentials_id);
