-- Custom script to add an additional column startedDate for holdings_status table.
-- Changes in this file will not result in an update of the function.
-- To change the function, update this script and copy it to the appropriate scripts.snippet field of the schema.json

CREATE OR REPLACE FUNCTION update_started_date_before_insertion()
  RETURNS TRIGGER
AS $$
 DECLARE
    started text;
    finished text;
    status text;
 BEGIN
   status = NEW.jsonb->'data'->'attributes'->'status'->>'name';

   IF status = 'Started' THEN
     started = '"' || CURRENT_TIMESTAMP(2) || '"';
     NEW.jsonb = jsonb_set(NEW.jsonb, '{data,attributes,started}' ,  started::jsonb);
   end IF;

   IF status = 'Completed' THEN

     finished = '"' || CURRENT_TIMESTAMP(2) || '"';
     NEW.jsonb = jsonb_set(NEW.jsonb, '{data,attributes,finished}' ,  finished::jsonb);
   end IF;

   IF status = 'In Progress' OR status = 'Completed' THEN

     started = to_json(OLD.jsonb->'data'->'attributes'->>'started');
     NEW.jsonb = jsonb_set(NEW.jsonb, '{data,attributes,started}' ,  started::jsonb);

   ELSE NEW.jsonb = NEW.jsonb;
   end IF;
 RETURN NEW;
 END;
$$
language 'plpgsql';

DROP TRIGGER IF EXISTS  update_started_date_before_insertion_trigger ON holdings_status CASCADE;

CREATE TRIGGER  update_started_date_before_insertion_trigger BEFORE UPDATE ON holdings_status
FOR EACH ROW EXECUTE PROCEDURE update_started_date_before_insertion();
