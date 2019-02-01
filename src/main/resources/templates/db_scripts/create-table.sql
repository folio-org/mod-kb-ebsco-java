ALTER TABLE tags
DROP COLUMN jsonb,
ADD COLUMN record_id VARCHAR (50) NOT NULL,
ADD COLUMN record_type VARCHAR (50) NOT NULL,
ADD COLUMN tag VARCHAR (50) NOT NULL;

INSERT INTO tags (record_id, record_type, tag) VALUES
('583-4345-762169', 'resource', 'first tag'),
('4345', 'package', 'second tag'),
('762169', 'title', 'third tag');
