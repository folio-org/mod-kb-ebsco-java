package org.folio.repository.tag;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.DbUtils.createParams;
import static org.folio.db.DbUtils.executeInTransaction;
import static org.folio.repository.DbUtil.DELETE_LOG_MESSAGE;
import static org.folio.repository.DbUtil.INSERT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.SELECT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.getTagsTableName;
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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.repository.RecordKey;
import org.folio.repository.RecordType;
import org.folio.rest.persist.PostgresClient;

@Component
class TagRepositoryImpl implements TagRepository {

  private static final Logger LOG = LoggerFactory.getLogger(TagRepositoryImpl.class);

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;


  @Override
  public CompletableFuture<List<DbTag>> findAll(String tenantId) {
    String query = String.format(SELECT_ALL_TAGS, getTagsTableName(tenantId));
    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTags);
  }

  @Override
  public CompletableFuture<List<DbTag>> findByRecord(String tenantId, String recordId, RecordType recordType) {
    JsonArray parameters = createParams(asList(recordId, recordType.getValue()));

    String query = String.format(SELECT_TAGS_BY_RECORD_ID_AND_RECORD_TYPE, getTagsTableName(tenantId));
    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query,parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTags);
  }

  @Override
  public CompletableFuture<List<DbTag>> findByRecordTypes(String tenantId, Set<RecordType> recordTypes) {
    if (isEmpty(recordTypes)) {
      return failedFuture(new IllegalArgumentException("At least one record type required"));
    }

    JsonArray parameters = createParams(toValues(recordTypes));
    String placeholders = createPlaceholders(parameters.size());

    String query = String.format(SELECT_TAGS_BY_RECORD_TYPES, getTagsTableName(tenantId), placeholders);
    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTags);
  }

  @Override
  public CompletableFuture<List<DbTag>> findByRecordByIds(String tenantId, List<String> recordIds,
      RecordType recordType) {
    if(isEmpty(recordIds)){
      return completedFuture(Collections.emptyList());
    }
    Future<ResultSet> resultSetFuture = findByRecordIdsOfType(tenantId, recordIds, recordType);

    return mapResult(resultSetFuture, this::readTags);
  }

  @Override
  public CompletableFuture<Map<String, List<DbTag>>> findPerRecord(String tenantId, List<String> recordIds,
      RecordType recordType) {
    if(isEmpty(recordIds)){
      return completedFuture(Collections.emptyMap());
    }
    Future<ResultSet> resultSetFuture = findByRecordIdsOfType(tenantId, recordIds, recordType);

    return mapResult(resultSetFuture, this::readTagsPerRecord).thenApply(this::remapToRecordIds);
  }

  @Override
  public CompletableFuture<Integer> countRecordsByTags(List<String> tags, RecordType recordType, String credentialsId,
      String tenantId) {
    return countRecordsByTagsAndPrefix(tags, "", tenantId, recordType);
  }

  @Override
  public CompletableFuture<Integer> countRecordsByTagsAndPrefix(List<String> tags, String recordIdPrefix,
      String tenantId, RecordType recordType) {
    if(isEmpty(tags)){
      return completedFuture(0);
    }
    JsonArray parameters = createParams(tags);
    parameters.add(recordType.getValue());
    parameters.add(recordIdPrefix + "%");

    String valuesList = createPlaceholders(tags.size());

    String query = String.format(COUNT_RECORDS_BY_TAG_VALUE_AND_TYPE_AND_RECORD_ID_PREFIX, getTagsTableName(tenantId),
      valuesList);
    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTagCount);
  }

  @Override
  public CompletableFuture<Boolean> updateRecordTags(String tenantId, String recordId, RecordType recordType,
      List<String> tags) {
    if(tags.isEmpty()){
      return unAssignTags(null, tenantId, recordId, recordType);
    }
    return updateTags(tenantId, recordId, recordType, tags);
  }

  @Override
  public CompletableFuture<Boolean> deleteRecordTags(String tenantId, String recordId, RecordType recordType) {
    return unAssignTags(null, tenantId, recordId, recordType);
  }

  @Override
  public CompletableFuture<List<String>> findDistinctRecordTags(String tenantId) {
    String query = String.format(SELECT_ALL_DISTINCT_TAGS, getTagsTableName(tenantId));
    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTagValues);
  }

  @Override
  public CompletableFuture<List<String>> findDistinctByRecordTypes(String tenantId, Set<RecordType> recordTypes) {
    if (isEmpty(recordTypes)) {
      return failedFuture(new IllegalArgumentException("At least one record type required"));
    }

    JsonArray parameters = createParams(toValues(recordTypes));
    String placeholders = createPlaceholders(parameters.size());

    String query = String.format(SELECT_DISTINCT_TAGS_BY_RECORD_TYPES, getTagsTableName(tenantId), placeholders);
    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::readTagValues);
  }

  private Future<ResultSet> findByRecordIdsOfType(String tenantId, List<String> recordIds, RecordType recordType) {
    String placeholders = createPlaceholders(recordIds.size());
    JsonArray parameters = createParametersWithRecordType(recordIds, recordType);

    String query = String.format(SELECT_TAGS_BY_RESOURCE_IDS, getTagsTableName(tenantId), placeholders);
    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<ResultSet> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return promise.future().recover(excTranslator.translateOrPassBy());
  }

  private List<String> readTagValues(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), object -> object.getString(TAG_COLUMN));
  }

  private List<DbTag> readTags(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), this::populateTag);
  }

  private Map<RecordKey, List<DbTag>> readTagsPerRecord(ResultSet resultSet) {
    return resultSet.getRows().stream().collect(
            groupingBy(this::readRecordKey, mapping(this::populateTag, toList())));
  }

  private RecordKey readRecordKey(JsonObject row) {
    return RecordKey.builder()
            .recordId(row.getString(RECORD_ID_COLUMN))
            .recordType(RecordType.fromValue(row.getString(RECORD_TYPE_COLUMN))).build();
  }

  private Integer readTagCount(ResultSet resultSet) {
    return resultSet.getRows().get(0).getInteger("count");
  }

  private Map<String, List<DbTag>> remapToRecordIds(Map<RecordKey, List<DbTag>> tagsPerRecord) {
    // method DOES NOT check that all records are of the same type, so be conscious
    Map<String, List<DbTag>> result = new HashMap<>();
    tagsPerRecord.forEach((recordKey, tags) -> result.put(recordKey.getRecordId(), tags));
    return result;
  }

  private DbTag populateTag(JsonObject row) {
    return DbTag.builder()
            .id(row.getString(ID_COLUMN))
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
    JsonArray parameters = new JsonArray();
    String updatedValues = createInsertStatement(recordId, recordType, tags, parameters);

    final String query = String.format(UPDATE_INSERT_STATEMENT_FOR_PROVIDER, getTagsTableName(tenantId), updatedValues);
    LOG.info(INSERT_LOG_MESSAGE, query);

    Promise<UpdateResult> promise = Promise.promise();
    postgresClient.execute(connection, query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  private CompletableFuture<Boolean> unAssignTags(AsyncResult<SQLConnection> connection, String tenantId, String recordId,
      RecordType recordType) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();

    final String deleteQuery = String.format(DELETE_TAG_RECORD, getTagsTableName(tenantId));
    LOG.info(DELETE_LOG_MESSAGE, deleteQuery);

    JsonArray parameters = createParams(asList(recordId, recordType.getValue()));

    if (connection != null) {
      pgClient(tenantId).execute(connection, deleteQuery, parameters, deleteHandler(future));
    } else {
      pgClient(tenantId).execute(deleteQuery, parameters, deleteHandler(future));
    }
    return future;
  }

  private Handler<AsyncResult<UpdateResult>> deleteHandler(CompletableFuture<Boolean> future) {
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

  private JsonArray createParametersWithRecordType(List<String> queryParameters, RecordType recordType) {
    JsonArray parameters = new JsonArray();

    queryParameters.forEach(parameters::add);
    parameters.add(recordType.getValue());

    return parameters;
  }

  private String createInsertStatement(String recordId, RecordType recordType, List<String> tags, JsonArray params) {
    tags.forEach(tag -> {
      String id = UUID.randomUUID().toString();
      params.add(id);
      params.add(recordId);
      params.add(recordType.getValue());
      params.add(tag);
    });
    return String.join(",", Collections.nCopies(tags.size(),"(?,?,?,?)"));
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
