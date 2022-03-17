package org.folio.repository.tag;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.common.LogUtils.logDeleteQueryInfoLevel;
import static org.folio.common.LogUtils.logInsertQueryInfoLevel;
import static org.folio.common.LogUtils.logSelectQueryInfoLevel;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.tag.TagTableConstants.COUNT_COLUMN;
import static org.folio.repository.tag.TagTableConstants.TAG_COLUMN;
import static org.folio.repository.tag.TagTableConstants.ID_COLUMN;
import static org.folio.repository.tag.TagTableConstants.RECORD_ID_COLUMN;
import static org.folio.repository.tag.TagTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.repository.tag.TagTableConstants.getCountRecordsByTagValueAndTypeAndRecordIdPrefix;
import static org.folio.repository.tag.TagTableConstants.deleteTagRecord;
import static org.folio.repository.tag.TagTableConstants.selectAllDistinctTags;
import static org.folio.repository.tag.TagTableConstants.selectAllTags;
import static org.folio.repository.tag.TagTableConstants.selectDistinctTagsByRecordTypes;
import static org.folio.repository.tag.TagTableConstants.selectTagsByRecordIdAndRecordType;
import static org.folio.repository.tag.TagTableConstants.selectTagsByRecordTypes;
import static org.folio.repository.tag.TagTableConstants.selectTagsByResourceIds;
import static org.folio.repository.tag.TagTableConstants.updateInsertStatementForProvider;
import static org.folio.util.FutureUtils.failedFuture;
import static org.folio.util.FutureUtils.mapResult;
import static org.folio.util.FutureUtils.mapVertxFuture;

import io.vertx.pgclient.PgConnection;
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
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.RowSetUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.repository.RecordKey;
import org.folio.repository.RecordType;
import org.folio.rest.model.filter.TagFilter;
import org.folio.rest.persist.PostgresClient;

@Component
class TagRepositoryImpl implements TagRepository {

  private static final Logger LOG = LogManager.getLogger(TagRepositoryImpl.class);

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public CompletableFuture<List<DbTag>> findAll(String tenantId) {
    String query = selectAllTags(tenantId);
    logSelectQueryInfoLevel(LOG, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTags);
  }

  @Override
  public CompletableFuture<List<DbTag>> findByRecord(String tenantId, String recordId, RecordType recordType) {
    Tuple parameters = Tuple.of(recordId, recordType.getValue());

    String query = selectTagsByRecordIdAndRecordType(tenantId);
    logSelectQueryInfoLevel(LOG, query, parameters);

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

    String query = selectTagsByRecordTypes(tenantId, placeholders);
    logSelectQueryInfoLevel(LOG, query, parameters);

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
      return unAssignTags(tenantId, recordId, recordType);
    }
    return updateTags(tenantId, recordId, recordType, tags);
  }

  @Override
  public CompletableFuture<Boolean> deleteRecordTags(String tenantId, String recordId, RecordType recordType) {
    return unAssignTags(tenantId, recordId, recordType);
  }

  @Override
  public CompletableFuture<Integer> countRecordsByTagFilter(TagFilter tagFilter, String tenantId) {
    List<String> tags = tagFilter.getTags();
    if (isEmpty(tags)) {
      return completedFuture(0);
    }
    Tuple parameters = createParams(tags);
    parameters.addString(tagFilter.getRecordType().getValue());
    parameters.addString(tagFilter.getRecordIdPrefix() + "%");

    String values = createPlaceholders(tags.size());

    String query = getCountRecordsByTagValueAndTypeAndRecordIdPrefix(tenantId, values);
    logSelectQueryInfoLevel(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTagCount);
  }

  @Override
  public CompletableFuture<List<String>> findDistinctRecordTags(String tenantId) {
    String query = selectAllDistinctTags(tenantId);
    logSelectQueryInfoLevel(LOG, query);

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

    String query = selectDistinctTagsByRecordTypes(tenantId, placeholders);
    logSelectQueryInfoLevel(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTagValues);
  }

  private Future<RowSet<Row>> findByRecordIdsOfType(String tenantId, List<String> recordIds, RecordType recordType) {
    String placeholders = createPlaceholders(recordIds.size());
    Tuple parameters = createParametersWithRecordType(recordIds, recordType);

    String query = selectTagsByResourceIds(tenantId, placeholders);
    logSelectQueryInfoLevel(LOG, query, parameters);

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
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    Future<Boolean> future = postgresClient.withTransaction(conn ->
        unAssignTags(conn, tenantId, recordId, recordType)
          .compose(aBoolean -> assignTags(conn, tenantId, recordId, recordType, tags)))
      .map(true);

    return mapVertxFuture(future);
  }

  private Future<Void> assignTags(PgConnection connection, String tenantId, String recordId,
                                  RecordType recordType, List<String> tags) {
    Tuple parameters = Tuple.tuple();
    String updatedValues = createInsertStatement(recordId, recordType, tags, parameters);

    final String query = updateInsertStatementForProvider(tenantId, updatedValues);
    logInsertQueryInfoLevel(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    connection
      .preparedQuery(query)
      .execute(parameters)
      .onComplete(promise);

    return promise.future().recover(excTranslator.translateOrPassBy()).map(nothing());
  }

  private Future<Boolean> unAssignTags(PgConnection connection, String tenantId, String recordId,
                                       RecordType recordType) {
    Promise<Boolean> promise = Promise.promise();

    final String query = deleteTagRecord(tenantId);
    Tuple parameters = Tuple.of(recordId, recordType.getValue());

    logDeleteQueryInfoLevel(LOG, query, parameters);

    connection
      .preparedQuery(query)
      .execute(parameters)
      .onComplete(deleteHandler(promise));

    return promise.future();
  }

  private CompletableFuture<Boolean> unAssignTags(String tenantId, String recordId, RecordType recordType) {
    PostgresClient client = PostgresClient.getInstance(vertx, tenantId);
    Future<Boolean> future = client
      .withConnection(conn -> unAssignTags(conn, tenantId, recordId, recordType));
    return mapVertxFuture(future);
  }

  private Handler<AsyncResult<RowSet<Row>>> deleteHandler(Promise<Boolean> promise) {
    return result -> {
      if (result.succeeded()) {
        LOG.info("Successfully deleted entries.");
        promise.complete(Boolean.TRUE);
      } else {
        LOG.info("Failed to delete entries.", result.cause());
        promise.fail(result.cause());
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
