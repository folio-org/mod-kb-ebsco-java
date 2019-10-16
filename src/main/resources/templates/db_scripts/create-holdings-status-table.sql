ALTER TABLE holdings_status
-- Add unique column with value true to ensure that only one row can be created in holdings_status
ADD COLUMN IF NOT EXISTS lock BOOL NOT NULL UNIQUE DEFAULT TRUE CHECK (lock);
