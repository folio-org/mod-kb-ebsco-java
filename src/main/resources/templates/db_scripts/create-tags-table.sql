ALTER TABLE tags
DROP COLUMN IF EXISTS jsonb,
ADD COLUMN IF NOT EXISTS record_id VARCHAR (50) NOT NULL,
ADD COLUMN IF NOT EXISTS record_type VARCHAR (10) CHECK (record_type IN ('provider', 'package', 'title', 'resource')),
ADD COLUMN IF NOT EXISTS tag VARCHAR (50) NOT NULL;

CREATE INDEX IF NOT EXISTS record_id_index ON tags (record_id);
CREATE INDEX IF NOT EXISTS tag_index ON tags (tag);
