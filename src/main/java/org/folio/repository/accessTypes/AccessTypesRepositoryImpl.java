package org.folio.repository.accessTypes;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.DbUtil.getAccessTypesTableName;
import static org.folio.repository.DbUtil.mapColumn;
import static org.folio.repository.accessTypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.accessTypes.AccessTypesTableConstants.SELECT_ALL_ACCESS_TYPES;
import static org.folio.repository.accessTypes.AccessTypesTableConstants.SELECT_COUNT_ACCESS_TYPES;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.persist.PostgresClient;

@Component
public class AccessTypesRepositoryImpl implements AccessTypesRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AccessTypesRepositoryImpl.class);
  private Vertx vertx;

  @Autowired
  private DBExceptionTranslator excTranslator;

  @Autowired
  public AccessTypesRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * Returns all access types for given tenantId.
   */
  @Override
  public CompletableFuture<List<AccessTypeCollectionItem>> findAll(String tenantId) {
    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(String.format(SELECT_ALL_ACCESS_TYPES, getAccessTypesTableName(tenantId)), promise);

    return mapResult(promise.future(), this::readAccessTypes);
  }

  /**
   * Returns an access type by given recordId.
   *
   * If note with given id doesn't exist then returns failed Future with NotFoundException as a cause.
   */
  @Override
  public CompletableFuture<AccessTypeCollectionItem> findById(String tenantId, String recordId) {
    return null;
  }

  /**
   * Saves a new access type record to the database
   *
   * @param accessType - current AccessType  {@link AccessTypeCollectionItem} object to save
   * @return
   */
  @Override
  public CompletableFuture<AccessTypeCollectionItem> save(AccessTypeCollectionItem accessType, String tenantId) {

    Promise<String> promise = Promise.promise();

    if (StringUtils.isBlank(accessType.getId())) {
      accessType.setId(UUID.randomUUID().toString());
    }
    LOG.info("Saving access type: " + accessType);
    pgClient(tenantId).save(ACCESS_TYPES_TABLE_NAME, accessType.getId(), accessType, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), accessTypeId -> {
      accessType.setId(accessTypeId);
      return accessType;
    });
  }

  @Override
  public CompletableFuture<Long> count(String tenantId) {
    Promise<ResultSet> promise = Promise.promise();

    String query = String.format(SELECT_COUNT_ACCESS_TYPES, getAccessTypesTableName(tenantId));
    LOG.info("Do count query: " + query);
    pgClient(tenantId).select(query, promise);

    return mapResult(promise.future(), rs -> rs.getResults().get(0).getLong(0));
  }

  private AccessTypeCollectionItem mapAccessItem(JsonObject row) {
    return mapColumn(row, "jsonb", AccessTypeCollectionItem.class).orElse(null);
  }

  private List<AccessTypeCollectionItem> readAccessTypes(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), this::mapAccessItem);
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
