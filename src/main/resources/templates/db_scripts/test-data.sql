INSERT INTO ${myuniversity}_${mymodule}.tags (id, record_id, record_type, tag) VALUES
('9c1e6f3c-682d-4af4-bd6b-20dad912ff94', '53-2767121-90099', 'resource', 'EBSCO'),
('103d6152-ce59-4793-b079-f2d216af7792', '53-2767121', 'package', 'folio'),
('ed0fdcac-1292-4354-adc2-7b80b58638e3', '413-1988660', 'package', 'spitfire'),
('52687cb3-ecd6-4570-a2e2-3e212f26bef8', '36-2728041', 'package', 'EBSCO')
ON CONFLICT DO NOTHING;
