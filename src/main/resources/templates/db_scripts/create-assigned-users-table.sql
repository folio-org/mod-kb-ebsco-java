ALTER TABLE assigned_users
DROP COLUMN IF EXISTS jsonb,
ADD COLUMN IF NOT EXISTS credentials_id uuid NOT NULL constraint fk_assigned_users_kb_credentials references kb_credentials,
ADD COLUMN IF NOT EXISTS user_name VARCHAR (100) NOT NULL,
ADD COLUMN IF NOT EXISTS first_name VARCHAR (100),
ADD COLUMN IF NOT EXISTS middle_name VARCHAR (100),
ADD COLUMN IF NOT EXISTS last_name VARCHAR (100) NOT NULL,
ADD COLUMN IF NOT EXISTS patron_group VARCHAR (100) NOT NULL;