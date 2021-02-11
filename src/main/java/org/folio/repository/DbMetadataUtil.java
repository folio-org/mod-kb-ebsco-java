package org.folio.repository;

import io.vertx.sqlclient.Row;

public final class DbMetadataUtil {

  public static final String CREATED_DATE_COLUMN = "created_date";
  public static final String UPDATED_DATE_COLUMN = "updated_date";
  public static final String CREATED_BY_USER_ID_COLUMN = "created_by_user_id";
  public static final String UPDATED_BY_USER_ID_COLUMN = "updated_by_user_id";
  public static final String CREATED_BY_USER_NAME_COLUMN = "created_by_user_name";
  public static final String UPDATED_BY_USER_NAME_COLUMN = "updated_by_user_name";

  private DbMetadataUtil() {
  }

  public static <T extends DbMetadata.DbMetadataBuilder<?, T>> T mapMetadata(DbMetadata.DbMetadataBuilder<?, T> builder,
                                                                             Row row) {
    return builder
      .createdDate(row.getOffsetDateTime(CREATED_DATE_COLUMN))
      .updatedDate(row.getOffsetDateTime(UPDATED_DATE_COLUMN))
      .createdByUserId(row.getUUID(CREATED_BY_USER_ID_COLUMN))
      .updatedByUserId(row.getUUID(UPDATED_BY_USER_ID_COLUMN))
      .createdByUserName(row.getString(CREATED_BY_USER_NAME_COLUMN))
      .updatedByUserName(row.getString(UPDATED_BY_USER_NAME_COLUMN));
  }
}
