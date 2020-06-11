package org.folio.repository.tag;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.common.LogUtils.*;
import static org.folio.db.DbUtils.createParams;
import static org.folio.db.DbUtils.executeInTransaction;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.tag.TagTableConstants.COUNT_COLUMN;
import static org.folio.repository.tag.TagTableConstants.COUNT_RECORDS_BY_TAG_VALUE_AND_TYPE_AND_RECORD_ID_PREFIX;
import static org.folio.repository.tag.TagTableConstants.DELETE_TAG_RECORD;
import static org.folio.repository.tag.TagTableConstants.ID_COLUMN;
import static org.folio.repository.tag.TagTableConstants.RECORD_ID_COLUMN;
import static org.folio.repository.tag.TagTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.repository.tag.TagTableConstants.SELECT_ALL_DISTINCT_TAGS;
import static org.folio.repository.tag.TagTableConstants.SELECT_ALL_TAGS;
import static org.folio.repository.tag.TagTableConstants.SELECT_DISTINCT_TAGS_BY_RECORD_TYPES;
import static org.folio.repository.tag.TagTableConstants.SELECT_TAGS_BY_RECORD_ID_AND_RECORD_TYPE;
import static org.folio.repository.tag.TagTableConstants.SELECT_TAGS_BY_RECORD_TYPES;
import static org.folio.repository.tag.TagTableConstants.SELECT_TAGS_BY_RESOURCE_IDS;
import static org.folio.repository.tag.TagTableConstants.TAG_COLUMN;
import static org.folio.repository.tag.TagTableConstants.UPDATE_INSERT_STATEMENT_FOR_PROVIDER;
import static org.folio.util.FutureUtils.failedFuture;
import static org.folio.util.FutureUtils.mapResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.RowSetUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.repository.RecordKey;
import org.folio.repository.RecordType;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.SQLConnection;

@Component
class TagRepositoryImpl implements TagRepository {

  private static final Logger LOG = LoggerFactory.getLogger(TagRepositoryImpl.class);

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public CompletableFuture<List<DbTag>> findAll(String tenantId) {
    String query = prepareQuery(SELECT_ALL_TAGS, getTagsTableName(tenantId));
    logSelectQuery(LOG, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTags);
  }

  @Override
  public CompletableFuture<List<DbTag>> findByRecord(String tenantId, String recordId, RecordType recordType) {
    Tuple parameters = Tuple.of(recordId, recordType.getValue());

    String query = prepareQuery(SELECT_TAGS_BY_RECORD_ID_AND_RECORD_TYPE, getTagsTableName(tenantId));
    logSelectQuery(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTags);
  }

  @Override
  public CompletableFuture<List<DbTag>> findByRecordTypes(String tenantId, Set<RecordType> recordTypes) {
    if (isEmpty(recordTypes)) {
      return failedFuture(new IllegalArgumentException("At least one record type required"));
    }

    Tuple parameters = createParams(toValues(recordTypes));
    String placeholders = createPlaceholders(parameters.size());

    String query = prepareQuery(SELECT_TAGS_BY_RECORD_TYPES, getTagsTableName(tenantId), placeholders);
    logSelectQuery(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTags);
  }

  @Override
  public CompletableFuture<List<DbTag>> findByRecordByIds(String tenantId, List<String> recordIds,
                                                          RecordType recordType) {
    if (isEmpty(recordIds)) {
      return completedFuture(Collections.emptyList());
    }
    Future<RowSet<Row>> resultSetFuture = findByRecordIdsOfType(tenantId, recordIds, recordType);

    return mapResult(resultSetFuture, this::readTags);
  }

  @Override
  public CompletableFuture<Map<String, List<DbTag>>> findPerRecord(String tenantId, List<String> recordIds,
                                                                   RecordType recordType) {
    if (isEmpty(recordIds)) {
      return completedFuture(Collections.emptyMap());
    }
    Future<RowSet<Row>> resultSetFuture = findByRecordIdsOfType(tenantId, recordIds, recordType);

    return mapResult(resultSetFuture, this::readTagsPerRecord).thenApply(this::remapToRecordIds);
  }

  @Override
  public CompletableFuture<Boolean> updateRecordTags(String tenantId, String recordId, RecordType recordType,
                                                     List<String> tags) {
    if (tags.isEmpty()) {
      return unAssignTags(null, tenantId, recordId, recordType);
    }
    return updateTags(tenantId, recordId, recordType, tags);
  }

  @Override
  public CompletableFuture<Boolean> deleteRecordTags(String tenantId, String recordId, RecordType recordType) {
    return unAssignTags(null, tenantId, recordId, recordType);
  }

  @Override
  public CompletableFuture<Integer> countRecordsByTags(List<String> tags, RecordType recordType, UUID credentialsId,
                                                       String tenantId) {
    return countRecordsByTagsAndPrefix(tags, "", tenantId, recordType);
  }

  @Override
  public CompletableFuture<Integer> countRecordsByTagsAndPrefix(List<String> tags, String recordIdPrefix,
                                                                String tenantId, RecordType recordType) {
    if (isEmpty(tags)) {
      return completedFuture(0);
    }
    Tuple parameters = createParams(tags);
    parameters.addString(recordType.getValue());
    parameters.addString(recordIdPrefix + "%");

    String values = createPlaceholders(tags.size());

    String query = prepareQuery(COUNT_RECORDS_BY_TAG_VALUE_AND_TYPE_AND_RECORD_ID_PREFIX, getTagsTableName(tenantId), values);
    logSelectQuery(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTagCount);
  }

  @Override
  public CompletableFuture<List<String>> findDistinctRecordTags(String tenantId) {
    String query = prepareQuery(SELECT_ALL_DISTINCT_TAGS, getTagsTableName(tenantId));
    logSelectQuery(LOG, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTagValues);
  }

  @Override
  public CompletableFuture<List<String>> findDistinctByRecordTypes(String tenantId, Set<RecordType> recordTypes) {
    if (isEmpty(recordTypes)) {
      return failedFuture(new IllegalArgumentException("At least one record type required"));
    }

    Tuple parameters = createParams(toValues(recordTypes));
    String placeholders = createPlaceholders(parameters.size());

    String query = prepareQuery(SELECT_DISTINCT_TAGS_BY_RECORD_TYPES, getTagsTableName(tenantId), placeholders);
    logSelectQuery(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTagValues);
  }

  private Future<RowSet<Row>> findByRecordIdsOfType(String tenantId, List<String> recordIds, RecordType recordType) {
    String placeholders = createPlaceholders(recordIds.size());
    Tuple parameters = createParametersWithRecordType(recordIds, recordType);

    String query = prepareQuery(SELECT_TAGS_BY_RESOURCE_IDS, getTagsTableName(tenantId), placeholders);
    logSelectQuery(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return promise.future().recover(excTranslator.translateOrPassBy());
  }

  private List<String> readTagValues(RowSet<Row> resultSet) {
    return RowSetUtils.mapItems(resultSet, row -> row.getString(TAG_COLUMN));
  }

  private List<DbTag> readTags(RowSet<Row> resultSet) {
    return RowSetUtils.mapItems(resultSet, this::populateTag);
  }

  private Map<RecordKey, List<DbTag>> readTagsPerRecord(RowSet<Row> resultSet) {
    return RowSetUtils.streamOf(resultSet)
      .collect(groupingBy(this::readRecordKey, mapping(this::populateTag, toList())));
  }

  private RecordKey readRecordKey(Row row) {
    return RecordKey.builder()
      .recordId(row.getString(RECORD_ID_COLUMN))
      .recordType(RecordType.fromValue(row.getString(RECORD_TYPE_COLUMN))).build();
  }

  private Integer readTagCount(RowSet<Row> resultSet) {
    return RowSetUtils.mapFirstItem(resultSet, row -> row.getInteger(COUNT_COLUMN));
  }

  private Map<String, List<DbTag>> remapToRecordIds(Map<RecordKey, List<DbTag>> tagsPerRecord) {
    // method DOES NOT check that all records are of the same type, so be conscious
    Map<String, List<DbTag>> result = new HashMap<>();
    tagsPerRecord.forEach((recordKey, tags) -> result.put(recordKey.getRecordId(), tags));
    return result;
  }

  private DbTag populateTag(Row row) {
    return DbTag.builder()
      .id(row.getUUID(ID_COLUMN))
      .recordId(row.getString(RECORD_ID_COLUMN))
      .recordType(RecordType.fromValue(row.getString(RECORD_TYPE_COLUMN)))
      .value(row.getString(TAG_COLUMN)).build();
  }

  private Set<String> toValues(Set<RecordType> recordTypes) {
    return recordTypes.stream().map(RecordType::getValue).collect(Collectors.toSet());
  }

  private CompletableFuture<Boolean> updateTags(String tenantId, String recordId, RecordType recordType,
                                                List<String> tags) {
    return executeInTransaction(tenantId, vertx,
      (postgresClient, connection) ->
        unAssignTags(connection, tenantId, recordId, recordType)
          .thenCompose(aBoolean -> assignTags(connection, tenantId, recordId, recordType, tags, postgresClient)))
      .thenApply(o -> true);
  }

  private CompletableFuture<Void> assignTags(AsyncResult<SQLConnection> connection, String tenantId, String recordId,
                                             RecordType recordType, List<String> tags, PostgresClient postgresClient) {
    Tuple parameters = Tuple.tuple();
    String updatedValues = createInsertStatement(recordId, recordType, tags, parameters);

    final String query = prepareQuery(UPDATE_INSERT_STATEMENT_FOR_PROVIDER, getTagsTableName(tenantId), updatedValues);
    logInsertQuery(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    postgresClient.execute(connection, query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  private CompletableFuture<Boolean> unAssignTags(AsyncResult<SQLConnection> connection, String tenantId, String recordId,
                                                  RecordType recordType) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();

    final String query = prepareQuery(DELETE_TAG_RECORD, getTagsTableName(tenantId));
    Tuple parameters = Tuple.of(recordId, recordType.getValue());

    logDeleteQuery(LOG, query, parameters);

    if (connection != null) {
      pgClient(tenantId).execute(connection, query, parameters, deleteHandler(future));
    } else {
      pgClient(tenantId).execute(query, parameters, deleteHandler(future));
    }
    return future;
  }

  private Handler<AsyncResult<RowSet<Row>>> deleteHandler(CompletableFuture<Boolean> future) {
    return result -> {
      if (result.succeeded()) {
        LOG.info("Successfully deleted entries.");
        future.complete(Boolean.TRUE);
      } else {
        LOG.info("Failed to delete entries." + result.cause());
        future.completeExceptionally(result.cause());
      }
    };
  }

  private Tuple createParametersWithRecordType(List<String> queryParameters, RecordType recordType) {
    Tuple parameters = Tuple.tuple();

    queryParameters.forEach(parameters::addString);
    parameters.addString(recordType.getValue());

    return parameters;
  }

  private String createInsertStatement(String recordId, RecordType recordType, List<String> tags, Tuple params) {
    tags.forEach(tag -> {
      params.addUUID(UUID.randomUUID());
      params.addString(recordId);
      params.addString(recordType.getValue());
      params.addString(tag);
    });
    return String.join(",", Collections.nCopies(tags.size(), "(?,?,?,?)"));
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
