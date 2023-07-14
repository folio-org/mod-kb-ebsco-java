CREATE OR REPLACE FUNCTION update_started_date_before_insertion()
  RETURNS TRIGGER
AS $$
 DECLARE
    started text;
    old_started text;
 BEGIN
   started = NEW.jsonb->'data'->'attributes'->>'started';
   old_started = OLD.jsonb->'data'->'attributes'->>'started';
   IF started IS NULL AND old_started IS NOT NULL THEN
     NEW.jsonb = jsonb_set(NEW.jsonb, '{data,attributes,started}', to_json(old_started)::jsonb);
   ELSE NEW.jsonb = NEW.jsonb;
   end IF;
 RETURN NEW;
 END;
$$
language 'plpgsql';

DROP TRIGGER IF EXISTS  update_started_date_before_insertion_trigger ON holdings_status CASCADE;

CREATE TRIGGER  update_started_date_before_insertion_trigger BEFORE UPDATE ON holdings_status
FOR EACH ROW EXECUTE PROCEDURE update_started_date_before_insertion();
