package org.folio.repository.tag;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.FutureUtils.mapVertxFuture;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.DbUtil.executeInTransaction;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.tag.TagTableConstants.COUNT_RECORDS_BY_TAG_VALUE_AND_TYPE_AND_RECORD_ID_PREFIX;
import static org.folio.repository.tag.TagTableConstants.DELETE_TAG_RECORD;
import static org.folio.repository.tag.TagTableConstants.ID_COLUMN;
import static org.folio.repository.tag.TagTableConstants.RECORD_ID_COLUMN;
import static org.folio.repository.tag.TagTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.repository.tag.TagTableConstants.SELECT_ALL_TAGS;
import static org.folio.repository.tag.TagTableConstants.SELECT_TAGS_BY_RECORD_ID_AND_RECORD_TYPE;
import static org.folio.repository.tag.TagTableConstants.SELECT_TAGS_BY_RECORD_TYPES;
import static org.folio.repository.tag.TagTableConstants.SELECT_TAGS_BY_RESOURCE_IDS;
import static org.folio.repository.tag.TagTableConstants.TAG_COLUMN;
import static org.folio.repository.tag.TagTableConstants.UPDATE_INSERT_STATEMENT_FOR_PROVIDER;

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
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.common.FutureUtils;
import org.folio.repository.RecordKey;
import org.folio.repository.RecordType;
import org.folio.rest.persist.PostgresClient;

@Component
class TagRepositoryImpl implements TagRepository {

  private static final Logger LOG = LoggerFactory.getLogger(TagRepositoryImpl.class);
  private Vertx vertx;

  @Autowired
  public TagRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<List<Tag>> findAll(String tenantId) {
    Future<ResultSet> resultSetFuture = Future.future();
    PostgresClient.getInstance(vertx, tenantId)
      .select(
        String.format(SELECT_ALL_TAGS, getTagsTableName(tenantId)),
        resultSetFuture.completer()
      );

    return mapResult(resultSetFuture, this::readTags);
  }

  @Override
  public CompletableFuture<List<Tag>> findByRecord(String tenantId, String recordId, RecordType recordType) {
    JsonArray parameters = createParameters(asList(recordId, recordType.getValue()));

    Future<ResultSet> resultSetFuture = Future.future();
    PostgresClient.getInstance(vertx, tenantId)
      .select(
        String.format(SELECT_TAGS_BY_RECORD_ID_AND_RECORD_TYPE, getTagsTableName(tenantId)),
        parameters, resultSetFuture.completer()
      );

    return mapResult(resultSetFuture, this::readTags);
  }

  @Override
  public CompletableFuture<List<Tag>> findByRecordTypes(String tenantId, Set<RecordType> recordTypes) {
    if (CollectionUtils.isEmpty(recordTypes)) {
      return FutureUtils.failedFuture(new IllegalArgumentException("At least one record type required"));
    }

    JsonArray parameters = createParameters(toValues(recordTypes));
    String placeholders = createPlaceholders(parameters.size());

    Future<ResultSet> resultSetFuture = Future.future();
    PostgresClient.getInstance(vertx, tenantId)
      .select(
        String.format(SELECT_TAGS_BY_RECORD_TYPES, getTagsTableName(tenantId), placeholders),
        parameters, resultSetFuture.completer()
      );

    return mapResult(resultSetFuture, this::readTags);
  }

  @Override
  public CompletableFuture<List<Tag>> findByRecordByIds(String tenantId, List<String> recordIds, RecordType recordType) {
    Future<ResultSet> resultSetFuture = findByRecordIdsOfType(tenantId, recordIds, recordType);

    return mapResult(resultSetFuture, this::readTags);
  }

  @Override
  public CompletableFuture<Map<String, List<Tag>>> findPerRecord(String tenantId, List<String> recordIds,
                                                                 RecordType recordType) {
    Future<ResultSet> resultSetFuture = findByRecordIdsOfType(tenantId, recordIds, recordType);

    return mapResult(resultSetFuture, this::readTagsPerRecord)
            .thenApply(this::remapToRecordIds);
  }

  @Override
  public CompletableFuture<Integer> countRecordsByTags(List<String> tags, String tenantId, RecordType recordType) {
    return countRecordsByTagsAndPrefix(tags, "", tenantId, recordType);
  }

  @Override
  public CompletableFuture<Integer> countRecordsByTagsAndPrefix(List<String> tags, String recordIdPrefix, String tenantId, RecordType recordType) {
    JsonArray parameters = createParameters(tags);
    parameters.add(recordType.getValue());
    parameters.add(recordIdPrefix + "%");

    String valuesList = createPlaceholders(tags.size());

    Future<ResultSet> future = Future.future();
    PostgresClient.getInstance(vertx, tenantId)
      .select(
        String.format(COUNT_RECORDS_BY_TAG_VALUE_AND_TYPE_AND_RECORD_ID_PREFIX, getTagsTableName(tenantId), valuesList),
        parameters,
        future.completer()
      );

    return mapResult(future, this::readTagCount);
  }

  @Override
  public CompletableFuture<Boolean> updateRecordTags(String tenantId, String recordId, RecordType recordType, List<String> tags) {
    if(tags.isEmpty()){
      return unAssignTags(null, tenantId, recordId, recordType);
    }
    return updateTags(tenantId, recordId, recordType, tags);
  }

  @Override
  public CompletableFuture<Boolean> deleteRecordTags(String tenantId, String recordId, RecordType recordType) {
    return unAssignTags(null, tenantId, recordId, recordType);
  }

  private Future<ResultSet> findByRecordIdsOfType(String tenantId, List<String> recordIds, RecordType recordType) {
    String placeholders = createPlaceholders(recordIds.size());
    JsonArray parameters = createParametersWithRecordType(recordIds, recordType);

    Future<ResultSet> resultSetFuture = Future.future();
    PostgresClient.getInstance(vertx, tenantId)
      .select(
        String.format(SELECT_TAGS_BY_RESOURCE_IDS, getTagsTableName(tenantId), placeholders),
        parameters, resultSetFuture.completer()
      );
    return resultSetFuture;
  }

  private List<Tag> readTags(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), this::populateTag);
  }

  private Map<RecordKey, List<Tag>> readTagsPerRecord(ResultSet resultSet) {
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

  private Map<String, List<Tag>> remapToRecordIds(Map<RecordKey, List<Tag>> tagsPerRecord) {
    // method DOES NOT check that all records are of the same type, so be conscious
    Map<String, List<Tag>> result = new HashMap<>();
    tagsPerRecord.forEach((recordKey, tags) -> result.put(recordKey.getRecordId(), tags));
    return result;
  }

  private Tag populateTag(JsonObject row) {
    return Tag.builder()
            .id(row.getString(ID_COLUMN))
            .recordId(row.getString(RECORD_ID_COLUMN))
            .recordType(RecordType.fromValue(row.getString(RECORD_TYPE_COLUMN)))
            .value(row.getString(TAG_COLUMN)).build();
  }

  private Set<String> toValues(Set<RecordType> recordTypes) {
    return recordTypes.stream().map(RecordType::getValue).collect(Collectors.toSet());
  }

  private CompletableFuture<Boolean> updateTags(String tenantId, String recordId, RecordType recordType, List<String> tags) {
    return executeInTransaction(tenantId, vertx,
      (postgresClient, connection) ->
        unAssignTags(connection, tenantId, recordId, recordType)
          .thenCompose(aBoolean -> assignTags(connection, tenantId, recordId, recordType, tags, postgresClient)))
      .thenApply(o -> true);
  }

  private CompletableFuture<Void> assignTags(AsyncResult<SQLConnection> connection, String tenantId, String recordId, RecordType recordType, List<String> tags, PostgresClient postgresClient) {
    JsonArray parameters = new JsonArray();
    String updatedValues = createInsertStatement(recordId, recordType, tags, parameters);
    final String query = String.format(UPDATE_INSERT_STATEMENT_FOR_PROVIDER, getTagsTableName(tenantId), updatedValues);
    LOG.info("Do insert query = " + query);

    Future<UpdateResult> future = Future.future();
    postgresClient.execute(connection, query, parameters, future.completer());
    return mapVertxFuture(future)
      .thenAccept(updateResult -> {});
  }

  private CompletableFuture<Boolean> unAssignTags(AsyncResult<SQLConnection> connection, String tenantId, String recordId,
      RecordType recordType) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    final String deleteQuery = String.format(DELETE_TAG_RECORD, getTagsTableName(tenantId));
    LOG.info("Do delete query = " + deleteQuery);

    JsonArray parameters = createParameters(asList(recordId, recordType.getValue()));
        if (connection != null) {
          PostgresClient.getInstance(vertx, tenantId)
            .execute(connection, deleteQuery, parameters, result -> {
            if (result.succeeded()) {
              LOG.info("Successfully deleted entries.");
              future.complete(Boolean.TRUE);
            } else {
              LOG.info("Failed to delete entries." + result.cause());
              future.completeExceptionally(result.cause());
          }
          });
        } else {
          PostgresClient.getInstance(vertx, tenantId).execute(deleteQuery, parameters, result -> {
            if (result.succeeded()) {
              LOG.info("Successfully deleted entries.");
              future.complete(Boolean.TRUE);
            } else {
              LOG.info("Failed to delete entries." + result.cause());
              future.completeExceptionally(result.cause());
            }
          });
        }
      return future;
  }

  private JsonArray createParametersWithRecordType(List<String> queryParameters, RecordType recordType) {
    JsonArray parameters = new JsonArray();

    queryParameters.forEach(parameters::add);
    parameters.add(recordType.getValue());

    return parameters;
  }

  private JsonArray createParameters(Iterable<?> queryParameters) {
    JsonArray parameters = new JsonArray();

    queryParameters.forEach(parameters::add);

    return parameters;
  }

  private String createPlaceholders(int size) {
    return StringUtils.join(Collections.nCopies(size, "?"), ", ");
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
}
