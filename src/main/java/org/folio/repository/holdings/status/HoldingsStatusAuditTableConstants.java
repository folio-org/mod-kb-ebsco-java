package org.folio.repository.holdings.status;

public class HoldingsStatusAuditTableConstants {
  public static final String HOLDINGS_STATUS_AUDIT_TABLE = "holdings_status_audit";
  public static final String JSONB_COLUMN = "jsonb";
  public static final String OPERATION_COLUMN = "operation";
  public static final String UPDATED_AT_COLUMN = "updated_at";
  public static final String DELETE_BEFORE_TIMESTAMP = "DELETE FROM %s WHERE " + UPDATED_AT_COLUMN + " < timestamp with time zone '%s';";

  private HoldingsStatusAuditTableConstants() { }
}
