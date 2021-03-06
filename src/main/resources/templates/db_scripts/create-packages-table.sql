ALTER TABLE packages
DROP COLUMN IF EXISTS jsonb,
ALTER COLUMN id TYPE VARCHAR (50),
ADD COLUMN IF NOT EXISTS name VARCHAR (200) NOT NULL,
ADD COLUMN IF NOT EXISTS content_type VARCHAR (50);

CREATE INDEX IF NOT EXISTS package_name_index ON packages (name);
