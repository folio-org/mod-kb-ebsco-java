ALTER TABLE holdings_status
-- Add unique column with value true to ensure that only one row can be created in holdings_status
ADD COLUMN IF NOT EXISTS lock BOOL NOT NULL UNIQUE DEFAULT TRUE CHECK (lock);

ALTER TABLE holdings_status
ADD COLUMN IF NOT EXISTS process_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';
ALTER TABLE holdings_status
ALTER COLUMN process_id DROP DEFAULT;
