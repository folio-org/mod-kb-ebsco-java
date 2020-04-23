CREATE OR REPLACE FUNCTION set_access_types_old_md_json()
    RETURNS TRIGGER
AS $$
 DECLARE
    createdDate timestamp WITH TIME ZONE;
    createdBy text ;
    updatedDate timestamp WITH TIME ZONE;
    updatedBy text ;
    injectedMetadata text;
    createdByUsername text;
    updatedByUsername text;
    creator text;
 BEGIN
   createdBy = OLD.jsonb->'metadata'->>'createdByUserId';
   createdDate = OLD.jsonb->'metadata'->>'createdDate';
   createdByUsername = OLD.jsonb->'metadata'->>'createdByUsername';
   creator = OLD.jsonb->'creator';
   updatedBy = NEW.jsonb->'metadata'->>'updatedByUserId';
   updatedDate = NEW.jsonb->'metadata'->>'updatedDate';
   updatedByUsername = NEW.jsonb->'metadata'->>'updatedByUsername';
   if createdBy ISNULL then     createdBy = 'undefined';   end if;
   if updatedBy ISNULL then     updatedBy = 'undefined';   end if;
   if createdByUsername ISNULL then     createdByUsername = 'undefined';   end if;
   if updatedByUsername ISNULL then     updatedByUsername = 'undefined';   end if;

   if createdDate IS NOT NULL
       then injectedMetadata = '{"createdDate":"'||to_char(createdDate,'YYYY-MM-DD"T"HH24:MI:SS.MSOF')||'" , "createdByUserId":"'||createdBy||'" , "createdByUsername":"'||createdByUsername||'", "updatedDate":"'||to_char(updatedDate,'YYYY-MM-DD"T"HH24:MI:SS.MSOF')||'" , "updatedByUserId":"'||updatedBy||'" , "updatedByUsername":"'|| updatedByUsername||'"}';
       NEW.jsonb = jsonb_set(NEW.jsonb, '{metadata}' ,  injectedMetadata::jsonb , false);
   else
     NEW.jsonb = NEW.jsonb;
   end if;

   if creator IS NOT null THEN
    NEW.jsonb = jsonb_set(NEW.jsonb, '{creator}',  creator::jsonb);
   end if;

 RETURN NEW;
 END;
$$
language 'plpgsql';

DROP TRIGGER IF EXISTS set_access_types_old_md_json_trigger ON access_types CASCADE;

CREATE TRIGGER set_access_types_old_md_json_trigger BEFORE UPDATE ON access_types   FOR EACH ROW EXECUTE PROCEDURE set_access_types_old_md_json();

DROP TRIGGER IF EXISTS set_access_types_old_md_trigger ON access_types CASCADE;

DROP FUNCTION IF EXISTS access_types_old_set_md() CASCADE;

ALTER TABLE access_types_old DROP COLUMN IF EXISTS created_by, DROP COLUMN IF EXISTS creation_date;
