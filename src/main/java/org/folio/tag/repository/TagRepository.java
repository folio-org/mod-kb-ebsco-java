package org.folio.tag.repository;

import static org.folio.tag.repository.TagTableConstants.DELETE_TAG_RECORD;
import static org.folio.tag.repository.TagTableConstants.SELECT_TAG_VALUES_BY_ID_AND_TYPE;
import static org.folio.tag.repository.TagTableConstants.TABLE_NAME;
import static org.folio.tag.repository.TagTableConstants.TAG_COLUMN;
import static org.folio.tag.repository.TagTableConstants.UPDATE_INSERT_STATEMENT_FOR_PROVIDER;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.persist.PostgresClient;
import org.folio.tag.RecordType;

@Component
public class TagRepository {
  private static final Logger LOG = LoggerFactory.getLogger(TagRepository.class);
  private Vertx vertx;

  @Autowired
  public TagRepository(Vertx vertx) {
    this.vertx = vertx;
  }

  public CompletableFuture<Tags> getTags(String tenantId, String recordId, RecordType recordType){
    CompletableFuture<Tags> future = new CompletableFuture<>();

    JsonArray parameters = createParameters(recordId, recordType.getValue());

    PostgresClient.getInstance(vertx, tenantId)
      .select(
        String.format(SELECT_TAG_VALUES_BY_ID_AND_TYPE, getTableName(tenantId)),
        parameters,
        result -> readTags(result, future)
      );
    return future;
  }

  private void readTags(AsyncResult<ResultSet> result, CompletableFuture<Tags> future) {
    ResultSet resultSet = result.result();
    List<String> tagValues = resultSet.getRows().stream()
      .map(row -> row.getString(TAG_COLUMN))
      .collect(Collectors.toList());
    future.complete(new Tags().withTagList(tagValues));
  }

  private String getTableName(String tenantId) {
    return PostgresClient.convertToPsqlStandard(tenantId) + "." + TABLE_NAME;
  }

  public CompletableFuture<Boolean> updateTags(String tenantId, String recordId, RecordType recordType, List<String> tags) {
    if(tags.isEmpty()){
      return unAssignTags(null, tenantId, recordId, recordType);
    }
    return assignTags(tenantId, recordId, recordType, tags);
  }

  private CompletableFuture<Boolean> assignTags(String tenantId, String recordId, RecordType recordType, List<String> tags) {
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
    final String deleteQuery = String.format(DELETE_TAG_RECORD, getTableName(tenantId), recordId, recordType);
    LOG.info("Do delete query = " + deleteQuery);

    JsonArray parameters = createParameters(recordId, recordType.getValue());
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

  private JsonArray createParameters(String ... queryParameters) {
    JsonArray parameters = new JsonArray();
    for (Object parameter : queryParameters) {
      parameters.add(parameter);
    }
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

}
