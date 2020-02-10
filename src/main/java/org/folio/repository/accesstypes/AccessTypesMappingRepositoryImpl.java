package org.folio.repository.accesstypes;

import static org.folio.common.FutureUtils.mapVertxFuture;
import static org.folio.repository.accesstypes.AccessTypesMappingTableConstants.INSERT_ACCESS_TYPE_MAPPING;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.UpdateResult;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
  public CompletableFuture<Boolean> save(AccessTypeMapping accessTypeMapping, String tenantId) {
    return saveMapping(accessTypeMapping.getAccessTypeId(), accessTypeMapping.getRecordId(),
      accessTypeMapping.getRecordType(), tenantId);
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

  @NotNull
  private JsonArray createInsertParams(String accessTypeId, String recordId, RecordType recordType) {
    JsonArray params = new JsonArray();
    params.add(UUID.randomUUID().toString());
    params.add(recordId);
    params.add(recordType.getValue());
    params.add(accessTypeId);
    return params;
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
