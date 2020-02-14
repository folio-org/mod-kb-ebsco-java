package org.folio.repository.accesstypes;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.getAccessTypesMappingTableName;
import static org.folio.repository.accesstypes.AccessTypesMappingTableConstants.ACCESS_TYPE_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesMappingTableConstants.DELETE_MAPPING_BY_RECORD_ID_AND_RECORD_TYPE;
import static org.folio.repository.accesstypes.AccessTypesMappingTableConstants.ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesMappingTableConstants.INSERT_MAPPING;
import static org.folio.repository.accesstypes.AccessTypesMappingTableConstants.RECORD_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesMappingTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.repository.accesstypes.AccessTypesMappingTableConstants.SELECT_MAPPING_BY_RECORD_ID_AND_RECORD_TYPE;
import static org.folio.repository.accesstypes.AccessTypesMappingTableConstants.UPDATE_MAPPING;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.List;
import java.util.Optional;
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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.repository.RecordType;
import org.folio.rest.persist.PostgresClient;

@Component
public class AccessTypesMappingRepositoryImpl implements AccessTypesMappingRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AccessTypesMappingRepositoryImpl.class);

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public CompletableFuture<AccessTypeMapping> save(AccessTypeMapping mapping, String tenantId) {
    Promise<UpdateResult> promise = Promise.promise();
    if (isNew(mapping)) {
      return persist(mapping, promise, tenantId);
    } else {
      return merge(mapping, promise, tenantId);
    }
  }

  @Override
  public CompletableFuture<Optional<AccessTypeMapping>> findByRecord(String recordId, RecordType recordType,
                                                                     String tenantId) {
    JsonArray params = createParams(asList(recordId, recordType.getValue()));
    String query = format(SELECT_MAPPING_BY_RECORD_ID_AND_RECORD_TYPE, getAccessTypesMappingTableName(tenantId));

    LOG.info("Do select query = {}", query);
    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future(), this::mapToAccessTypeMapping);
  }

  @Override
  public CompletableFuture<Void> deleteByRecord(String recordId, RecordType recordType, String tenantId) {
    JsonArray params = createParams(asList(recordId, recordType.getValue()));
    String query = format(DELETE_MAPPING_BY_RECORD_ID_AND_RECORD_TYPE, getAccessTypesMappingTableName(tenantId));

    LOG.info("Do delete query = {}", query);
    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);

    return mapVertxFuture(promise.future().recover(excTranslator.translateOrPassBy())).thenApply(updateResult -> null);
  }

  private CompletableFuture<AccessTypeMapping> merge(AccessTypeMapping mapping, Promise<UpdateResult> promise,
                                                     String tenantId) {
    JsonArray params = createParams(asList(mapping.getAccessTypeId(), mapping.getId()));
    String query = format(UPDATE_MAPPING, getAccessTypesMappingTableName(tenantId));

    LOG.info("Do update query = {}", query);
    pgClient(tenantId).execute(query, params, promise);

    return mapVertxFuture(promise.future().recover(excTranslator.translateOrPassBy())).thenApply(updateResult -> mapping);
  }

  private CompletableFuture<AccessTypeMapping> persist(AccessTypeMapping mapping, Promise<UpdateResult> promise,
                                                       String tenantId) {
    final AccessTypeMapping m = mapping.toBuilder().id(UUID.randomUUID().toString()).build();
    JsonArray params = createInsertParams(m);
    String query = format(INSERT_MAPPING, getAccessTypesMappingTableName(tenantId));

    LOG.info("Do insert query = {}", query);
    pgClient(tenantId).execute(query, params, promise);

    return mapVertxFuture(promise.future().recover(excTranslator.translateOrPassBy())).thenApply(updateResult -> m);
  }

  private boolean isNew(AccessTypeMapping mapping) {
    return StringUtils.isBlank(mapping.getId());
  }

  private Optional<AccessTypeMapping> mapToAccessTypeMapping(ResultSet resultSet) {
    final List<JsonObject> rows = resultSet.getRows();

    return rows.isEmpty() ? Optional.empty() : Optional.of(mapAccessItem(rows.get(0)));
  }

  private AccessTypeMapping mapAccessItem(JsonObject row) {
    return AccessTypeMapping.builder()
      .id(row.getString(ID_COLUMN))
      .accessTypeId(row.getString(ACCESS_TYPE_ID_COLUMN))
      .recordId(row.getString(RECORD_ID_COLUMN))
      .recordType(RecordType.fromValue(row.getString(RECORD_TYPE_COLUMN)))
      .build();
  }

  @NotNull
  private JsonArray createInsertParams(AccessTypeMapping mapping) {
    return createParams(asList(
      mapping.getId(),
      mapping.getRecordId(),
      mapping.getRecordType().getValue(),
      mapping.getAccessTypeId()
    ));
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
