ALTER TABLE holdings_status_audit
DROP COLUMN IF EXISTS id,
ADD COLUMN IF NOT EXISTS operation VARCHAR(20) NOT NULL,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;