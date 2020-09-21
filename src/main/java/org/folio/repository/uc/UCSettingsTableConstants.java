package org.folio.repository.uc;

import static org.folio.repository.DbMetadataUtil.CREATED_BY_USER_ID_COLUMN;
import static org.folio.repository.DbMetadataUtil.CREATED_BY_USER_NAME_COLUMN;
import static org.folio.repository.DbMetadataUtil.CREATED_DATE_COLUMN;
import static org.folio.repository.DbMetadataUtil.UPDATED_BY_USER_ID_COLUMN;
import static org.folio.repository.DbMetadataUtil.UPDATED_BY_USER_NAME_COLUMN;
import static org.folio.repository.DbMetadataUtil.UPDATED_DATE_COLUMN;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;

public final class UCSettingsTableConstants {

  public static final String UC_SETTINGS_TABLE_NAME = "usage_consolidation_settings";

  public static final String ID_COLUMN = "id";
  public static final String KB_CREDENTIALS_ID_COLUMN = "kb_credentials_id";
  public static final String CUSTOMER_KEY_COLUMN = "customer_key";
  public static final String CURRENCY_COLUMN = "currency";
  public static final String START_MONTH_COLUMN = "start_month";
  public static final String PLATFORM_TYPE_COLUMN = "platform_type";

  public static final String SELECT_UC_SETTINGS_BY_CREDENTIALS_ID;
  public static final String INSERT_UC_SETTINGS;

  static {
    String[] allColumns = new String[] {
      ID_COLUMN,
      KB_CREDENTIALS_ID_COLUMN,
      CUSTOMER_KEY_COLUMN,
      CURRENCY_COLUMN,
      START_MONTH_COLUMN,
      PLATFORM_TYPE_COLUMN,
      CREATED_DATE_COLUMN,
      CREATED_BY_USER_ID_COLUMN,
      CREATED_BY_USER_NAME_COLUMN,
      UPDATED_DATE_COLUMN,
      UPDATED_BY_USER_ID_COLUMN,
      UPDATED_BY_USER_NAME_COLUMN
    };
    SELECT_UC_SETTINGS_BY_CREDENTIALS_ID = selectQuery() + " " + whereQuery(KB_CREDENTIALS_ID_COLUMN) + ";";
    INSERT_UC_SETTINGS = insertQuery(allColumns) + ";";
  }

  private UCSettingsTableConstants() {
  }
}
