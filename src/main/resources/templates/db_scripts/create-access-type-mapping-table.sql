ALTER TABLE access_types_mappings
  DROP COLUMN IF EXISTS jsonb,
  ADD COLUMN IF NOT EXISTS record_id VARCHAR (50) NOT NULL,
  ADD COLUMN IF NOT EXISTS record_type VARCHAR (10) NOT NULL,
  ADD COLUMN IF NOT EXISTS access_type_id UUID NOT NULL;

CREATE INDEX IF NOT EXISTS index_record_id ON access_types_mappings (record_id);

ALTER TABLE access_types_mappings
  DROP CONSTRAINT IF EXISTS fk_access_types,
  ADD CONSTRAINT fk_access_types FOREIGN KEY (access_type_id) REFERENCES access_types (id);

ALTER TABLE access_types_mappings
  DROP CONSTRAINT IF EXISTS unique_mapping_record_id,
  ADD CONSTRAINT unique_mapping_record_id UNIQUE(record_id);

ALTER TABLE access_types_mappings
  DROP CONSTRAINT IF EXISTS check_access_types_mappings_record_type,
  ADD CONSTRAINT check_access_types_mappings_record_type CHECK (record_type IN ('provider', 'package', 'title', 'resource'));
