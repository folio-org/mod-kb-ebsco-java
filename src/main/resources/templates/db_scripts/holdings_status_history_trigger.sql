-- Trigger that records all insert, update, delete operations for table holdings_status
CREATE OR REPLACE FUNCTION process_holdings_status_audit() RETURNS TRIGGER AS $$
	DECLARE
    previous_search_path text := current_setting('search_path');
    BEGIN
        PERFORM set_config('search_path', TG_TABLE_SCHEMA, true);
        IF (TG_OP = 'DELETE') THEN
            INSERT INTO holdings_status_audit(operation, updated_at, jsonb, credentials_id) SELECT 'DELETE', now(), OLD.jsonb, OLD.credentials_id;
        ELSIF (TG_OP = 'UPDATE') THEN
            INSERT INTO holdings_status_audit(operation, updated_at, jsonb, credentials_id) SELECT 'UPDATE', now(), NEW.jsonb, NEW.credentials_id;
        ELSIF (TG_OP = 'INSERT') THEN
            INSERT INTO holdings_status_audit(operation, updated_at, jsonb, credentials_id) SELECT 'INSERT', now(), NEW.jsonb, NEW.credentials_id;
        END IF;
		    PERFORM set_config('search_path', previous_search_path, true);
        RETURN NULL;
    END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS  holdings_status_audit ON holdings_status CASCADE;

CREATE TRIGGER holdings_status_audit
AFTER INSERT OR UPDATE OR DELETE ON holdings_status
    FOR EACH ROW EXECUTE PROCEDURE process_holdings_status_audit();
