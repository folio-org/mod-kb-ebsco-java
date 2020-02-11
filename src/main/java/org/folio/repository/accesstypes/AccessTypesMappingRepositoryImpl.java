package org.folio.repository.accesstypes;

import static java.util.Arrays.asList;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.FutureUtils.mapVertxFuture;
import static org.folio.repository.DbUtil.createParameters;
import static org.folio.repository.DbUtil.getAccessTypesMappingTableName;
import static org.folio.repository.accesstypes.AccessTypesMappingTableConstants.ACCESS_TYPE_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesMappingTableConstants.ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesMappingTableConstants.INSERT_ACCESS_TYPE_MAPPING;
import static org.folio.repository.accesstypes.AccessTypesMappingTableConstants.RECORD_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesMappingTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesMappingTableConstants.SELECT_MAPPING_BY_RECORD_ID_AND_RECORD_TYPE;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.repository.RecordType;
import org.folio.rest.persist.PostgresClient;

@Component
public class AccessTypesMappingRepositoryImpl implements AccessTypesMappingRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AccessTypesMappingRepositoryImpl.class);

  private Vertx vertx;

  @Autowired
  public AccessTypesMappingRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<Boolean> save(AccessTypeInDb accessTypeMapping, String tenantId) {

    JsonArray params = createInsertParams(accessTypeMapping);

    Promise<UpdateResult> promise = Promise.promise();
    String query = String.format(INSERT_ACCESS_TYPE_MAPPING, getAccessTypesMappingTableName(tenantId));
    LOG.info("Do insert query = {}", query);
    pgClient(tenantId).execute(query, params, promise);
    return mapVertxFuture(promise.future()).thenApply(updateResult -> updateResult.getUpdated() == 1);
  }

  @Override
  public CompletableFuture<AccessTypeInDb> findByRecordIdAndRecordType(String recordId, RecordType recordType, String tenantId) {
    JsonArray parameters = createParameters(asList(recordId, recordType.getValue()));

    Promise<ResultSet> promise = Promise.promise();
    final String query = String.format(SELECT_MAPPING_BY_RECORD_ID_AND_RECORD_TYPE, getAccessTypesMappingTableName(tenantId));
    LOG.info("Do select query = {}", query);
    PostgresClient.getInstance(vertx, tenantId).select(query, parameters, promise);

    return mapResult(promise.future(), this::mapToAccessTypeInDb);
  }

  private AccessTypeInDb mapToAccessTypeInDb(ResultSet resultSet ) {
    final List<JsonObject> rows = resultSet.getRows();
    if (rows.isEmpty()){
      return null;
    }
    return mapAccessItem(rows.get(0));
  }

  private AccessTypeInDb mapAccessItem(JsonObject row ) {
    return AccessTypeInDb.builder()
      .id(row.getString(ID_COLUMN))
      .accessTypeId(row.getString(ACCESS_TYPE_ID_COLUMN))
      .recordId(row.getString(RECORD_ID_COLUMN))
      .recordType(RecordType.fromValue(row.getString(RECORD_TYPE_COLUMN)))
      .build();
  }

  @NotNull
  private JsonArray createInsertParams(AccessTypeInDb accessTypeInDb) {
    JsonArray params = new JsonArray();
    params.add(UUID.randomUUID().toString());
    params.add(accessTypeInDb.getRecordId());
    params.add(accessTypeInDb.getRecordType().getValue());
    params.add(accessTypeInDb.getAccessTypeId());
    return params;
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
