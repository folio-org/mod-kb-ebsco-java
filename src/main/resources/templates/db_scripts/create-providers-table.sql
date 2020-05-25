ALTER TABLE providers
DROP COLUMN IF EXISTS jsonb,
ALTER COLUMN id TYPE VARCHAR (50),
ADD COLUMN IF NOT EXISTS credentials_id UUID NOT NULL CONSTRAINT providers_kb_credentials_fkey REFERENCES kb_credentials,
ADD COLUMN IF NOT EXISTS name VARCHAR (200) NOT NULL;

CREATE INDEX IF NOT EXISTS provider_name_index ON providers (name);

ALTER TABLE providers
  DROP CONSTRAINT IF EXISTS providers_pkey,
  ADD CONSTRAINT providers_pkey PRIMARY KEY (id, credentials_id);
