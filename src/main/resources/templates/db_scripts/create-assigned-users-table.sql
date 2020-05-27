ALTER TABLE assigned_users
  DROP COLUMN IF EXISTS jsonb,
  ADD COLUMN IF NOT EXISTS credentials_id UUID NOT NULL,
  ADD COLUMN IF NOT EXISTS user_name VARCHAR (100) NOT NULL,
  ADD COLUMN IF NOT EXISTS first_name VARCHAR (100),
  ADD COLUMN IF NOT EXISTS middle_name VARCHAR (100),
  ADD COLUMN IF NOT EXISTS last_name VARCHAR (100) NOT NULL,
  ADD COLUMN IF NOT EXISTS patron_group VARCHAR (100) NOT NULL;

ALTER TABLE assigned_users
  DROP CONSTRAINT IF EXISTS fk_assigned_users_kb_credentials,
  ADD CONSTRAINT fk_assigned_users_kb_credentials FOREIGN KEY (credentials_id) REFERENCES kb_credentials (id);
