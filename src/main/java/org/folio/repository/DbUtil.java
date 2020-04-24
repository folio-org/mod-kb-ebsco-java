package org.folio.repository;

import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME_OLD;
import static org.folio.repository.assigneduser.AssignedUsersConstants.ASSIGNED_USERS_TABLE_NAME;
import static org.folio.repository.holdings.HoldingsTableConstants.HOLDINGS_TABLE;
import static org.folio.repository.holdings.status.HoldingsStatusAuditTableConstants.HOLDINGS_STATUS_AUDIT_TABLE;
import static org.folio.repository.holdings.status.HoldingsStatusTableConstants.HOLDINGS_STATUS_TABLE;
import static org.folio.repository.holdings.status.RetryStatusTableConstants.RETRY_STATUS_TABLE;
import static org.folio.repository.holdings.status.TransactionIdTableConstants.TRANSACTION_ID_TABLE;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.packages.PackageTableConstants.PACKAGES_TABLE_NAME;
import static org.folio.repository.providers.ProviderTableConstants.PROVIDERS_TABLE_NAME;
import static org.folio.repository.resources.ResourceTableConstants.RESOURCES_TABLE_NAME;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.repository.titles.TitlesTableConstants.TITLES_TABLE_NAME;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.UpdateResult;

import org.folio.db.exc.ConstraintViolationException;
import org.folio.db.exc.DbExcUtils;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.ObjectMapperTool;

public class DbUtil {

  private DbUtil() {
  }

  public static JsonArray createInsertOrUpdateParameters(String id, String name) {
    return new JsonArray()
      .add(id)
      .add(name)
      .add(name);
  }

  private static String getTableName(String tenantId, String tableName) {
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

  public static String getAccessTypesOldTableName(String tenantId) {
    return getTableName(tenantId, ACCESS_TYPES_TABLE_NAME_OLD);
  }

  public static String getAccessTypesTableName(String tenantId) {
    return getTableName(tenantId, ACCESS_TYPES_TABLE_NAME);
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

  public static <T> Optional<T> mapColumn(JsonObject row, String columnName, Class<T> tClass) {
    try {
      return Optional.of(ObjectMapperTool.getMapper().readValue(row.getString(columnName), tClass));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  public static Function<Throwable, Future<UpdateResult>> uniqueConstraintRecover(String columnName, Throwable t) {
    return throwable -> DbExcUtils.isUniqueViolation(throwable)
      && ((ConstraintViolationException) throwable).getConstraint().getColumns().contains(columnName)
      ? Future.failedFuture(t)
      : Future.failedFuture(throwable);
  }

  public static Function<Throwable, Future<UpdateResult>> foreignKeyConstraintRecover(Throwable t) {
    return throwable -> DbExcUtils.isFKViolation(throwable)
      ? Future.failedFuture(t)
      : Future.failedFuture(throwable);
  }
}
