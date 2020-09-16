package org.folio.repository;

import static io.vertx.core.Future.failedFuture;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isEqualCollection;

import static org.folio.db.exc.DbExcUtils.isFKViolation;
import static org.folio.db.exc.DbExcUtils.isPKViolation;
import static org.folio.db.exc.DbExcUtils.isUniqueViolation;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_VIEW_NAME;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_VIEW_NAME;
import static org.folio.repository.currencies.CurrenciesConstants.CURRENCIES_TABLE_NAME;
import static org.folio.repository.holdings.HoldingsTableConstants.HOLDINGS_TABLE;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.HOLDINGS_STATUS_TABLE;
import static org.folio.repository.holdings.status.audit.HoldingsStatusAuditTableConstants.HOLDINGS_STATUS_AUDIT_TABLE;
import static org.folio.repository.holdings.status.retry.RetryStatusTableConstants.RETRY_STATUS_TABLE;
import static org.folio.repository.holdings.transaction.TransactionIdTableConstants.TRANSACTION_ID_TABLE;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.packages.PackageTableConstants.PACKAGES_TABLE_NAME;
import static org.folio.repository.providers.ProviderTableConstants.PROVIDERS_TABLE_NAME;
import static org.folio.repository.resources.ResourceTableConstants.RESOURCES_TABLE_NAME;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.repository.titles.TitlesTableConstants.TITLES_TABLE_NAME;
import static org.folio.repository.users.UsersTableConstants.USERS_TABLE_NAME;

import java.util.List;
import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

import org.folio.db.exc.ConstraintViolationException;
import org.folio.rest.persist.PostgresClient;

public class DbUtil {

  public static final String CREATED_DATE_COLUMN = "created_date";
  public static final String UPDATED_DATE_COLUMN = "updated_date";
  public static final String CREATED_BY_USER_ID_COLUMN = "created_by_user_id";
  public static final String UPDATED_BY_USER_ID_COLUMN = "updated_by_user_id";
  public static final String CREATED_BY_USER_NAME_COLUMN = "created_by_user_name";
  public static final String UPDATED_BY_USER_NAME_COLUMN = "updated_by_user_name";

  private DbUtil() {
  }

  public static PostgresClient pgClient(String tenantId, Vertx vertx) {
    return PostgresClient.getInstance(vertx, tenantId);
  }

  public static String getTableName(String tenantId, String tableName) {
    return PostgresClient.convertToPsqlStandard(tenantId) + "." + tableName;
  }

  public static String getTitlesTableName(String tenantId) {
    return getTableName(tenantId, TITLES_TABLE_NAME);
  }

  public static String getResourcesTableName(String tenantId) {
    return getTableName(tenantId, RESOURCES_TABLE_NAME);
  }

  public static String getProviderTableName(String tenantId) {
    return getTableName(tenantId, PROVIDERS_TABLE_NAME);
  }

  public static String getPackagesTableName(String tenantId) {
    return getTableName(tenantId, PACKAGES_TABLE_NAME);
  }

  public static String getTagsTableName(String tenantId) {
    return getTableName(tenantId, TAGS_TABLE_NAME);
  }

  public static String getHoldingsTableName(String tenantId) {
    return getTableName(tenantId, HOLDINGS_TABLE);
  }

  public static String getHoldingsStatusTableName(String tenantId) {
    return getTableName(tenantId, HOLDINGS_STATUS_TABLE);
  }

  public static String getHoldingsStatusAuditTableName(String tenantId) {
    return getTableName(tenantId, HOLDINGS_STATUS_AUDIT_TABLE);
  }

  public static String getRetryStatusTableName(String tenantId) {
    return getTableName(tenantId, RETRY_STATUS_TABLE);
  }

  public static String getTransactionIdTableName(String tenantId) {
    return getTableName(tenantId, TRANSACTION_ID_TABLE);
  }

  public static String getAccessTypesTableName(String tenantId) {
    return getTableName(tenantId, ACCESS_TYPES_TABLE_NAME);
  }

  public static String getAccessTypesViewName(String tenantId) {
    return getTableName(tenantId, ACCESS_TYPES_VIEW_NAME);
  }

  public static String getAccessTypesMappingTableName(String tenantId) {
    return getTableName(tenantId, ACCESS_TYPES_MAPPING_TABLE_NAME);
  }

  public static String getKbCredentialsTableName(String tenantId) {
    return getTableName(tenantId, KB_CREDENTIALS_TABLE_NAME);
  }

  public static String getAssignedUsersTableName(String tenantId) {
    return getTableName(tenantId, ASSIGNED_USERS_TABLE_NAME);
  }

  public static String getAssignedUsersViewName(String tenantId) {
    return getTableName(tenantId, ASSIGNED_USERS_VIEW_NAME);
  }

  public static String getCurrenciesTableName(String tenantId) {
    return getTableName(tenantId, CURRENCIES_TABLE_NAME);
  }

  public static String getUsersTableName(String tenantId) {
    return getTableName(tenantId, USERS_TABLE_NAME);
  }

  public static Function<Throwable, Future<RowSet<Row>>> uniqueConstraintRecover(String columnName, Throwable t) {
    return uniqueConstraintRecover(singletonList(columnName), t);
  }

  public static Function<Throwable, Future<RowSet<Row>>> uniqueConstraintRecover(List<String> columnNames, Throwable t) {
    return throwable -> isUniqueViolation(throwable)
      && isEqualCollection(((ConstraintViolationException) throwable).getConstraint().getColumns(), columnNames)
      ? failedFuture(t)
      : failedFuture(throwable);
  }

  public static Function<Throwable, Future<RowSet<Row>>> pkConstraintRecover(String columnName, Throwable t) {
    return pkConstraintRecover(singletonList(columnName), t);
  }

  public static Function<Throwable, Future<RowSet<Row>>> pkConstraintRecover(List<String> columnNames, Throwable t) {
    return throwable -> isPKViolation(throwable)
      && isEqualCollection(((ConstraintViolationException) throwable).getConstraint().getColumns(), columnNames)
      ? failedFuture(t)
      : failedFuture(throwable);
  }

  public static Function<Throwable, Future<RowSet<Row>>> foreignKeyConstraintRecover(Throwable t) {
    return throwable -> isFKViolation(throwable)
      ? failedFuture(t)
      : failedFuture(throwable);
  }

  public static String prepareQuery(String queryTemplate, Object... params) {
    String query = String.format(queryTemplate, params);
    StringBuilder sb = new StringBuilder(query);
    int index = 1;
    int i = 0;
    while ((i = sb.indexOf("?", i)) != -1) {
      sb.replace(i, i + 1, "$" + index++);
    }
    return sb.toString();
  }

  public static <T extends DbMetadata.DbMetadataBuilder<?, T>> T mapMetadata(DbMetadata.DbMetadataBuilder<?, T> builder, Row row) {
    return builder
      .createdDate(row.getOffsetDateTime(CREATED_DATE_COLUMN))
      .updatedDate(row.getOffsetDateTime(UPDATED_DATE_COLUMN))
      .createdByUserId(row.getUUID(CREATED_BY_USER_ID_COLUMN))
      .updatedByUserId(row.getUUID(UPDATED_BY_USER_ID_COLUMN))
      .createdByUserName(row.getString(CREATED_BY_USER_NAME_COLUMN))
      .updatedByUserName(row.getString(UPDATED_BY_USER_NAME_COLUMN));
  }
}
