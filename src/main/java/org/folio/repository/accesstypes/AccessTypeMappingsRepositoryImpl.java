package org.folio.repository.accesstypes;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.getAccessTypesMappingTableName;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPE_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.DELETE_MAPPING_BY_RECORD_ID_AND_RECORD_TYPE;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.RECORD_ID_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.SELECT_ALL_MAPPINGS;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.SELECT_MAPPING_BY_ACCESS_TYPE_ID;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.SELECT_MAPPING_BY_RECORD_ID_AND_RECORD_TYPE;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.UPSERT_MAPPING;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.repository.RecordType;
import org.folio.rest.persist.PostgresClient;

@Component
public class AccessTypeMappingsRepositoryImpl implements AccessTypeMappingsRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AccessTypeMappingsRepositoryImpl.class);

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public CompletableFuture<Collection<AccessTypeMapping>> findAll(String tenantId) {
    String query = format(SELECT_ALL_MAPPINGS, getAccessTypesMappingTableName(tenantId));

    LOG.info("Do select query = {}", query);
    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, promise);

    return mapResult(promise.future(), resultSet -> mapItems(resultSet.getRows(), this::mapAccessItem));
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
  public CompletableFuture<Collection<AccessTypeMapping>> findByAccessTypeId(String accessTypeId, String tenantId) {
    JsonArray params = createParams(Collections.singletonList(accessTypeId));
    String query = format(SELECT_MAPPING_BY_ACCESS_TYPE_ID, getAccessTypesMappingTableName(tenantId));

    LOG.info("Do select query = {}", query);
    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, params, promise);

    return mapResult(promise.future(), resultSet -> mapItems(resultSet.getRows(), this::mapAccessItem));
  }

  @Override
  public CompletableFuture<AccessTypeMapping> save(AccessTypeMapping mapping, String tenantId) {

    JsonArray params = createUpsertParams(mapping);
    String query = format(UPSERT_MAPPING, getAccessTypesMappingTableName(tenantId));

    LOG.info("Do insert query = {}", query);
    Promise<UpdateResult> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);

    return mapVertxFuture(promise.future().recover(excTranslator.translateOrPassBy())).thenApply(updateResult -> mapping);
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
  private JsonArray createUpsertParams(AccessTypeMapping mapping) {
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
