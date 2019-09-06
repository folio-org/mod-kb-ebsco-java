-- Custom script to add an additional column startedDate for holdings_status table.
-- Changes in this file will not result in an update of the function.
-- To change the function, update this script

CREATE OR REPLACE FUNCTION update_started_date_before_insertion()
  RETURNS TRIGGER
AS $$
 DECLARE
    started text;
 BEGIN
   started = NEW.jsonb->'data'->'attributes'->>'started';
   IF started IS NULL THEN
     NEW.jsonb = jsonb_set(NEW.jsonb, '{data,attributes,started}' ,  to_json(OLD.jsonb->'data'->'attributes'->>'started')::jsonb);
   ELSE NEW.jsonb = NEW.jsonb;
   end IF;
 RETURN NEW;
 END;
$$
language 'plpgsql';

DROP TRIGGER IF EXISTS  update_started_date_before_insertion_trigger ON holdings_status CASCADE;

CREATE TRIGGER  update_started_date_before_insertion_trigger BEFORE UPDATE ON holdings_status
FOR EACH ROW EXECUTE PROCEDURE update_started_date_before_insertion();
