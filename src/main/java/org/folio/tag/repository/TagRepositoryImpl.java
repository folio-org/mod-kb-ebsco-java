package org.folio.tag.repository;

import static java.util.Arrays.asList;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.tag.repository.TagTableConstants.DELETE_TAG_RECORD;
import static org.folio.tag.repository.TagTableConstants.ID_COLUMN;
import static org.folio.tag.repository.TagTableConstants.RECORD_ID_COLUMN;
import static org.folio.tag.repository.TagTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.tag.repository.TagTableConstants.SELECT_ALL_TAGS;
import static org.folio.tag.repository.TagTableConstants.SELECT_TAGS_BY_RECORD_ID_AND_RECORD_TYPE;
import static org.folio.tag.repository.TagTableConstants.SELECT_TAGS_BY_RECORD_TYPES;
import static org.folio.tag.repository.TagTableConstants.TABLE_NAME;
import static org.folio.tag.repository.TagTableConstants.TAG_COLUMN;
import static org.folio.tag.repository.TagTableConstants.UPDATE_INSERT_STATEMENT_FOR_PROVIDER;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.common.FutureUtils;
import org.folio.rest.persist.PostgresClient;
import org.folio.tag.RecordType;
import org.folio.tag.Tag;

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
        String.format(SELECT_ALL_TAGS, getTableName(tenantId)),
        resultSetFuture.completer()
      );

    return mapResultSet(resultSetFuture, this::readTags);
  }

  @Override
  public CompletableFuture<List<Tag>> findByRecord(String tenantId, String recordId, RecordType recordType) {
    JsonArray parameters = createParameters(asList(recordId, recordType.getValue()));

    Future<ResultSet> resultSetFuture = Future.future();
    PostgresClient.getInstance(vertx, tenantId)
      .select(
        String.format(SELECT_TAGS_BY_RECORD_ID_AND_RECORD_TYPE, getTableName(tenantId)),
        parameters, resultSetFuture.completer()
      );

    return mapResultSet(resultSetFuture, this::readTags);
  }

  @Override
  public CompletableFuture<List<Tag>> findByRecordTypes(String tenantId, Set<RecordType> recordTypes) {
    if (CollectionUtils.isEmpty(recordTypes)) {
      return FutureUtils.failedFuture(new IllegalArgumentException("At least one record type required"));
    }

    JsonArray parameters = createParameters(toValues(recordTypes));
    String placeholders = createPlaceholders(parameters);

    Future<ResultSet> resultSetFuture = Future.future();
    PostgresClient.getInstance(vertx, tenantId)
      .select(
        String.format(SELECT_TAGS_BY_RECORD_TYPES, getTableName(tenantId), placeholders),
        parameters, resultSetFuture.completer()
      );

    return mapResultSet(resultSetFuture, this::readTags);
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

  private <T> CompletableFuture<T> mapResultSet(Future<ResultSet> future, Function<ResultSet, T> mapper) {
    CompletableFuture<T> result = new CompletableFuture<>();

    future.map(mapper)
      .map(result::complete)
      .otherwise(result::completeExceptionally);

    return result;
  }

  private List<Tag> readTags(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), this::populateTag);
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

  private String getTableName(String tenantId) {
    return PostgresClient.convertToPsqlStandard(tenantId) + "." + TABLE_NAME;
  }

  private CompletableFuture<Boolean> updateTags(String tenantId, String recordId, RecordType recordType, List<String> tags) {
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    MutableObject<AsyncResult<SQLConnection>> mutableConnection = new MutableObject<>();
    CompletableFuture<Boolean> future = CompletableFuture.completedFuture(null);
    CompletableFuture<Boolean> rollbackFuture = new CompletableFuture<>();
    return future
      .thenCompose(o -> {
        CompletableFuture<Boolean> startTxFuture = new CompletableFuture<>();
        postgresClient.startTx(connection -> {
          mutableConnection.setValue(connection);
          startTxFuture.complete(null);
        });
        return startTxFuture;
      })
      .thenCompose(aBoolean -> unAssignTags(mutableConnection.getValue(), tenantId, recordId, recordType))
      .thenCompose(aBoolean -> assignTags(mutableConnection.getValue(), tenantId, recordId, recordType, tags, postgresClient))
      .whenComplete((result, ex) -> {
        if(ex != null) {
          LOG.info("Transaction was not successful. Roll back changes.");
          postgresClient.rollbackTx(mutableConnection.getValue(), rollback -> rollbackFuture.completeExceptionally(ex));
        } else {
          rollbackFuture.complete(result);
        }
      })
      .thenCombine(rollbackFuture, (o, aBoolean) -> true);
  }

  private CompletableFuture<Boolean> assignTags(AsyncResult<SQLConnection> connection, String tenantId, String recordId, RecordType recordType, List<String> tags, PostgresClient postgresClient) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    JsonArray parameters = new JsonArray();
    String updatedValues = createInsertStatement(recordId, recordType, tags, parameters);
    final String query = String.format(UPDATE_INSERT_STATEMENT_FOR_PROVIDER, getTableName(tenantId), updatedValues);
    LOG.info("Do insert query = " + query);
    postgresClient.execute(connection, query, parameters, reply -> {
      if (reply.succeeded()) {
        LOG.info("Successfully inserted entries.");
        postgresClient.endTx(connection, done -> future.complete(Boolean.TRUE));
      } else {
        LOG.info("Failed to insert entries");
        future.completeExceptionally(reply.cause());
      }
    });
    return future;
  }

  private CompletableFuture<Boolean> unAssignTags(AsyncResult<SQLConnection> connection, String tenantId, String recordId,
      RecordType recordType) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    final String deleteQuery = String.format(DELETE_TAG_RECORD, getTableName(tenantId));
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

  private JsonArray createParameters(Iterable<?> queryParameters) {
    JsonArray parameters = new JsonArray();

    queryParameters.forEach(parameters::add);

    return parameters;
  }

  private String createPlaceholders(JsonArray parameters) {
    return StringUtils.join(Collections.nCopies(parameters.size(), "?"), ", ");
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
