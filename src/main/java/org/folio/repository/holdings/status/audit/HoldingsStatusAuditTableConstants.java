package org.folio.repository.holdings.status.audit;

import static org.folio.repository.DbUtil.getHoldingsStatusAuditTableName;
import static org.folio.repository.DbUtil.prepareQuery;

public final class HoldingsStatusAuditTableConstants {
  public static final String HOLDINGS_STATUS_AUDIT_TABLE = "holdings_status_audit";
  public static final String JSONB_COLUMN = "jsonb";
  public static final String OPERATION_COLUMN = "operation";
  public static final String UPDATED_AT_COLUMN = "updated_at";
  public static final String CREDENTIALS_COLUMN = "credentials_id";

  private HoldingsStatusAuditTableConstants() {
  }

  public static String deleteBeforeTimestampForCredentials(String tenantId) {
    return prepareQuery(deleteBeforeTimestampForCredentials(), getHoldingsStatusAuditTableName(tenantId));
  }

  private static String deleteBeforeTimestampForCredentials() {
    return "DELETE FROM %s WHERE " + UPDATED_AT_COLUMN + " < ? AND " + CREDENTIALS_COLUMN + "=?;";
  }
}
