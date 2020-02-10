package org.folio.repository.accesstypes;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.FutureUtils.mapVertxFuture;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPE_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.INSERT_ACCESS_TYPE_MAPPING;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.RECORD_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.SELECT_ACCESS_TYPE_MAPPING;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.SELECT_ACCESS_TYPE_MAPPING_BY_RECORD_ID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.common.ListUtils;
import org.folio.repository.DbUtil;
import org.folio.repository.RecordType;
import org.folio.rest.persist.PostgresClient;

@Component
public class AccessTypesMappingRepositoryImpl implements AccessTypesMappingRepository {

  private Vertx vertx;

  @Autowired
  public AccessTypesMappingRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<List<AccessTypeMapping>> findAll(String tenantId) {
    Promise<ResultSet> promise = Promise.promise();
    String selectQuery = String.format(SELECT_ACCESS_TYPE_MAPPING, DbUtil.getAccessTypesMappingTableName(tenantId));

    pgClient(tenantId).select(selectQuery, promise);
    return mapResult(promise.future(), this::readAccessTypeMappings);
  }

  @Override
  public CompletableFuture<Optional<AccessTypeMapping>> findByRecord(String recordId, RecordType recordType, String tenantId) {
    Promise<ResultSet> promise = Promise.promise();
    String selectQuery = String.format(SELECT_ACCESS_TYPE_MAPPING_BY_RECORD_ID, DbUtil.getAccessTypesMappingTableName(tenantId));

    JsonArray params = new JsonArray();
    params.add(recordId);
    params.add(recordType.getValue());

    pgClient(tenantId).select(selectQuery, params, promise);

    return mapResult(promise.future(), this::readSingleAccessTypeMapping);
  }

  private Optional<AccessTypeMapping> readSingleAccessTypeMapping(ResultSet resultSet) {
    List<JsonObject> rows = resultSet.getRows();
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(populateAccessTypeMapping(rows.get(0)));
  }

  private List<AccessTypeMapping> readAccessTypeMappings(ResultSet resultSet) {
    return ListUtils.mapItems(resultSet.getRows(), this::populateAccessTypeMapping);
  }

  private AccessTypeMapping populateAccessTypeMapping(JsonObject object) {
    return AccessTypeMapping.builder()
      .id(object.getString(ID_COLUMN))
      .recordId(object.getString(RECORD_ID_COLUMN))
      .recordType(RecordType.fromValue(object.getString(RECORD_TYPE_COLUMN)))
      .accessTypeId(object.getString(ACCESS_TYPE_ID_COLUMN))
      .build();
  }

  @Override
  public CompletableFuture<Boolean> saveMapping(AccessTypeMapping accessTypeMapping, String tenantId) {
    return saveMapping(accessTypeMapping.getAccessTypeId(), accessTypeMapping.getRecordId(),
      accessTypeMapping.getRecordType(), tenantId);
  }

  @NotNull
  private JsonArray createInsertParams(String accessTypeId, String recordId, RecordType recordType) {
    JsonArray params = new JsonArray();
    params.add(UUID.randomUUID().toString());
    params.add(recordId);
    params.add(recordType.getValue());
    params.add(accessTypeId);
    return params;
  }

  private CompletableFuture<Boolean> saveMapping(String accessTypeId, String recordId, RecordType recordType,
                                                 String tenantId) {
    JsonArray params = createInsertParams(accessTypeId, recordId, recordType);

    Promise<UpdateResult> promise = Promise.promise();
    String insert = String.format(INSERT_ACCESS_TYPE_MAPPING, DbUtil.getAccessTypesMappingTableName(tenantId));

    pgClient(tenantId).execute(insert, params, promise);
    return mapVertxFuture(promise.future())
      .thenApply(updateResult -> updateResult.getUpdated() == 1);
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }

}
