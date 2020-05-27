ALTER TABLE packages
DROP COLUMN IF EXISTS jsonb,
ALTER COLUMN id TYPE VARCHAR (50),
ADD COLUMN IF NOT EXISTS credentials_id UUID NOT NULL CONSTRAINT packages_kb_credentials_fkey REFERENCES kb_credentials,
ADD COLUMN IF NOT EXISTS name VARCHAR (200) NOT NULL,
ADD COLUMN IF NOT EXISTS content_type VARCHAR (50);

CREATE INDEX IF NOT EXISTS package_name_index ON packages (name);

ALTER TABLE packages
  DROP CONSTRAINT IF EXISTS packages_pkey,
  ADD CONSTRAINT packages_pkey PRIMARY KEY (id, credentials_id);
