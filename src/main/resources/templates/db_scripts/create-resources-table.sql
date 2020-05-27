ALTER TABLE resources
DROP COLUMN IF EXISTS jsonb,
ALTER COLUMN id TYPE VARCHAR (50),
ADD COLUMN IF NOT EXISTS credentials_id UUID NOT NULL CONSTRAINT resources_kb_credentials_fkey REFERENCES kb_credentials,
ADD COLUMN IF NOT EXISTS name VARCHAR (200) NOT NULL;

CREATE INDEX IF NOT EXISTS resource_name_index ON resources(name);

ALTER TABLE resources
  DROP CONSTRAINT IF EXISTS resources_pkey,
  ADD CONSTRAINT resources_pkey PRIMARY KEY (id, credentials_id);
