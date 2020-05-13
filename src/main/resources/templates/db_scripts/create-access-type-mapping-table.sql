ALTER TABLE access_types_mappings
DROP COLUMN IF EXISTS jsonb,
ADD COLUMN IF NOT EXISTS record_id VARCHAR (50) NOT NULL,
ADD COLUMN IF NOT EXISTS record_type VARCHAR (10) CHECK (record_type IN ('provider', 'package', 'title', 'resource')),
ADD COLUMN IF NOT EXISTS access_type_id UUID NOT NULL CONSTRAINT access_types_fkey REFERENCES access_types;

CREATE INDEX IF NOT EXISTS record_id_index ON access_types_mappings (record_id);
