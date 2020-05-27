ALTER TABLE access_types
  DROP COLUMN IF EXISTS jsonb,
  ADD COLUMN IF NOT EXISTS credentials_id UUID NOT NULL,
  ADD COLUMN IF NOT EXISTS name VARCHAR (100) NOT NULL,
  ADD COLUMN IF NOT EXISTS description VARCHAR (200),
  ADD COLUMN IF NOT EXISTS created_date TIMESTAMP WITH TIME ZONE NOT NULL,
  ADD COLUMN IF NOT EXISTS created_by_user_id UUID NOT NULL,
  ADD COLUMN IF NOT EXISTS created_by_username VARCHAR (100) NOT NULL,
  ADD COLUMN IF NOT EXISTS created_by_last_name VARCHAR (100) NOT NULL,
  ADD COLUMN IF NOT EXISTS created_by_first_name VARCHAR (100),
  ADD COLUMN IF NOT EXISTS created_by_middle_name VARCHAR (100),
  ADD COLUMN IF NOT EXISTS updated_date TIMESTAMP WITH TIME ZONE,
  ADD COLUMN IF NOT EXISTS updated_by_user_id UUID,
  ADD COLUMN IF NOT EXISTS updated_by_username VARCHAR (100),
  ADD COLUMN IF NOT EXISTS updated_by_last_name VARCHAR (100),
  ADD COLUMN IF NOT EXISTS updated_by_first_name VARCHAR (100),
  ADD COLUMN IF NOT EXISTS updated_by_middle_name VARCHAR (100);

ALTER TABLE access_types
  DROP CONSTRAINT IF EXISTS fk_access_types_kb_credentials,
  ADD CONSTRAINT fk_access_types_kb_credentials FOREIGN KEY (credentials_id) REFERENCES kb_credentials (id);

ALTER TABLE access_types
  DROP CONSTRAINT IF EXISTS unique_name,
  ADD CONSTRAINT unique_name UNIQUE(name);

CREATE INDEX IF NOT EXISTS name_index ON access_types (name);
