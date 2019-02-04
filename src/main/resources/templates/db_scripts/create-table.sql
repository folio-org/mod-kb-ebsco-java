ALTER TABLE tags
DROP COLUMN jsonb,
ADD COLUMN record_id VARCHAR (50) NOT NULL,
ADD COLUMN record_type VARCHAR (50) NOT NULL,
ADD COLUMN tag VARCHAR (50) NOT NULL;

CREATE INDEX record_id_index ON tags (record_id);
CREATE INDEX tag_index ON tags (tag);
