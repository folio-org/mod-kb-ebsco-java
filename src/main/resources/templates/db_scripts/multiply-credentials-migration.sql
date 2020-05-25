CREATE TABLE IF NOT EXISTS ${myuniversity}_${mymodule}.kb_credentials
(
  id UUID CONSTRAINT pk_kb_credentials PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  api_key VARCHAR(100) NOT NULL,
  url VARCHAR(100) NOT NULL,
  customer_id VARCHAR(100) NOT NULL,
  created_date TIMESTAMPTZ NOT NULL,
  created_by_user_id UUID NOT NULL,
  created_by_user_name VARCHAR(100) NOT NULL,
  updated_date TIMESTAMPTZ,
  updated_by_user_id UUID,
  updated_by_user_name VARCHAR(100)
);

INSERT INTO ${myuniversity}_${mymodule}.kb_credentials(id, name, api_key, url, customer_id, created_date, created_by_user_id, created_by_user_name)
  VALUES ('2ae17e64-dd9e-4a5f-8fdc-ab013c5a5db3'::uuid,
  'Dummy Credentials',
  'dummyKey',
  'http://dummy.url.com',
  'dummyCustomerId',
  CURRENT_TIMESTAMP,
  '00000000-0000-0000-0000-000000000000'::uuid,
  'SYSTEM'
 );

CREATE OR REPLACE FUNCTION ${myuniversity}_${mymodule}.get_credentials_id() RETURNS UUID AS $$
  DECLARE row_count INT;
  BEGIN
    row_count = (SELECT count(*) FROM ${myuniversity}_${mymodule}.kb_credentials);
    IF row_count = 1 THEN
      RETURN (SELECT id FROM ${myuniversity}_${mymodule}.kb_credentials);
    END IF;
  END;
$$ LANGUAGE 'plpgsql';

-- accessTypes --
CREATE TABLE IF NOT EXISTS ${myuniversity}_${mymodule}.access_types
(
  id UUID CONSTRAINT pk_access_types PRIMARY KEY,
  jsonb JSONB
);

DROP TRIGGER IF EXISTS set_access_types_md_json_trigger ON ${myuniversity}_${mymodule}.access_types;
DROP FUNCTION IF EXISTS ${myuniversity}_${mymodule}.set_access_types_md_json;
DROP TRIGGER IF EXISTS set_id_in_jsonb ON ${myuniversity}_${mymodule}.access_types;
DROP FUNCTION IF EXISTS ${myuniversity}_${mymodule}.set_id_in_jsonb;

ALTER TABLE ${myuniversity}_${mymodule}.access_types
	ADD COLUMN credentials_id UUID CONSTRAINT fk_access_types_kb_credentials REFERENCES ${myuniversity}_${mymodule}.kb_credentials,
	ADD COLUMN name VARCHAR(75),
	ADD COLUMN description VARCHAR(150),
	ADD COLUMN created_by_first_name VARCHAR(256),
	ADD COLUMN created_by_middle_name VARCHAR(256),
	ADD COLUMN created_by_last_name VARCHAR(256),
	ADD COLUMN updated_by_first_name VARCHAR(256),
	ADD COLUMN updated_by_middle_name VARCHAR(256),
	ADD COLUMN updated_by_last_name VARCHAR(256),
	ADD COLUMN created_date TIMESTAMPTZ,
	ADD COLUMN updated_date TIMESTAMPTZ,
	ADD COLUMN created_by_user_id UUID ,
	ADD COLUMN updated_by_user_id UUID ,
	ADD COLUMN created_by_username VARCHAR(256),
	ADD COLUMN updated_by_username VARCHAR(256);

UPDATE ${myuniversity}_${mymodule}.access_types SET
  credentials_id = (SELECT ${myuniversity}_${mymodule}.get_credentials_id()),
  name = jsonb -> 'attributes' ->> 'name',
  description = jsonb -> 'attributes' ->> 'description',
  created_by_first_name = jsonb -> 'creator' ->> 'firstName',
  created_by_middle_name = jsonb -> 'creator' ->> 'middleName',
  created_by_last_name = jsonb -> 'creator' ->> 'lastName',
  updated_by_first_name = jsonb -> 'updater' ->> 'firstName',
  updated_by_middle_name = jsonb -> 'updater' ->> 'middleName',
  updated_by_last_name = jsonb -> 'updater' ->> 'lastName',
  created_date = to_timestamp(jsonb -> 'metadata' ->> 'createdDate', 'YYYY-MM-DD"T"HH24:MI:SS.MS'),
  updated_date = to_timestamp(jsonb -> 'metadata' ->> 'updatedDate', 'YYYY-MM-DD"T"HH24:MI:SS.MS'),
  created_by_user_id = (jsonb -> 'metadata' ->> 'createdByUserId')::uuid,
  updated_by_user_id = (jsonb -> 'metadata' ->> 'updatedByUserId')::uuid,
  created_by_username = jsonb -> 'metadata' ->> 'createdByUsername',
  updated_by_username = jsonb -> 'metadata' ->> 'updatedByUsername';

ALTER TABLE ${myuniversity}_${mymodule}.access_types
  ALTER COLUMN credentials_id SET NOT NULL,
  ALTER COLUMN name SET NOT NULL,
  ALTER COLUMN created_date SET NOT NULL,
  ALTER COLUMN created_by_username SET NOT NULL,
  ALTER COLUMN created_by_last_name SET NOT NULL;

ALTER TABLE ${myuniversity}_${mymodule}.access_types
  DROP CONSTRAINT IF EXISTS fk_access_types_kb_credentials,
  ADD CONSTRAINT fk_access_types_kb_credentials FOREIGN KEY (credentials_id) REFERENCES ${myuniversity}_${mymodule}.kb_credentials;

ALTER TABLE ${myuniversity}_${mymodule}.access_types
  DROP CONSTRAINT IF EXISTS unique_name,
  ADD CONSTRAINT unique_name UNIQUE(name);

ALTER TABLE ${myuniversity}_${mymodule}.access_types
  DROP COLUMN IF EXISTS jsonb;

CREATE INDEX IF NOT EXISTS idx_access_types_credentials_id ON ${myuniversity}_${mymodule}.access_types (credentials_id);
CREATE INDEX IF NOT EXISTS idx_access_types_name ON ${myuniversity}_${mymodule}.access_types (name);
-- accessTypes --

-- accessTypeMapping --
CREATE TABLE IF NOT EXISTS ${myuniversity}_${mymodule}.access_types_mapping
(
  id UUID CONSTRAINT pk_access_types_mapping PRIMARY KEY,
  record_id VARCHAR (50) NOT NULL,
  record_type VARCHAR (10),
  access_type_id VARCHAR (50) NOT NULL
);

ALTER TABLE ${myuniversity}_${mymodule}.access_types_mapping
  ALTER COLUMN access_type_id type UUID USING access_type_id::uuid;

ALTER TABLE ${myuniversity}_${mymodule}.access_types_mapping
  DROP CONSTRAINT IF EXISTS fk_access_types_mapping_to_access_types,
  ADD CONSTRAINT fk_access_types_mapping_to_access_types FOREIGN KEY (access_type_id) REFERENCES ${myuniversity}_${mymodule}.access_types;

ALTER TABLE ${myuniversity}_${mymodule}.access_types_mapping
  DROP CONSTRAINT IF EXISTS unique_mapping_record_id;

ALTER TABLE ${myuniversity}_${mymodule}.access_types_mapping
  DROP CONSTRAINT IF EXISTS check_access_types_mappings_record_type,
  ADD CONSTRAINT check_access_types_mappings_record_type CHECK (record_type IN ('provider', 'package', 'title', 'resource'));

CREATE INDEX IF NOT EXISTS index_record_id ON ${myuniversity}_${mymodule}.access_types_mapping (record_id);
-- accessTypeMapping --
