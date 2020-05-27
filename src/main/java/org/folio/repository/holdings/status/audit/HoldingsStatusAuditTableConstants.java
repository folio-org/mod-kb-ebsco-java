package org.folio.repository.holdings.status.audit;

public class HoldingsStatusAuditTableConstants {
  public static final String HOLDINGS_STATUS_AUDIT_TABLE = "holdings_status_audit";
  public static final String JSONB_COLUMN = "jsonb";
  public static final String OPERATION_COLUMN = "operation";
  public static final String UPDATED_AT_COLUMN = "updated_at";
  public static final String CREDENTIALS_COLUMN = "credentials_id";
  public static final String DELETE_BEFORE_TIMESTAMP_FOR_CREDENTIALS = "DELETE FROM %s WHERE " + UPDATED_AT_COLUMN + " < timestamp with time zone '%s' AND " + CREDENTIALS_COLUMN + "=?;";

  private HoldingsStatusAuditTableConstants() { }
}
