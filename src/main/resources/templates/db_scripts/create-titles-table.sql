ALTER TABLE titles
DROP COLUMN IF EXISTS jsonb,
ALTER COLUMN id TYPE VARCHAR (50),
ADD COLUMN IF NOT EXISTS credentials_id UUID NOT NULL CONSTRAINT titles_kb_credentials_fkey REFERENCES kb_credentials,
ADD COLUMN IF NOT EXISTS name VARCHAR (200) NOT NULL;

CREATE INDEX IF NOT EXISTS titles_name_index ON titles (name);

ALTER TABLE titles
  DROP CONSTRAINT IF EXISTS titles_pkey,
  ADD CONSTRAINT titles_pkey PRIMARY KEY (id, credentials_id);
