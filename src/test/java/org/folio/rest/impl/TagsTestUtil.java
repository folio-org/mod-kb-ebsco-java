package org.folio.rest.impl;

import static org.folio.tag.repository.TagTableConstants.ID_COLUMN;
import static org.folio.tag.repository.TagTableConstants.RECORD_ID_COLUMN;
import static org.folio.tag.repository.TagTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.tag.repository.TagTableConstants.TABLE_NAME;
import static org.folio.tag.repository.TagTableConstants.TAG_COLUMN;
import static org.folio.util.TestUtil.STUB_TENANT;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;

import org.folio.rest.persist.PostgresClient;
import org.folio.tag.RecordType;

public class TagsTestUtil {
  private TagsTestUtil() {
  }

  public static void insertTag(Vertx vertx, String recordId, final RecordType recordType, String value) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "INSERT INTO " + PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TABLE_NAME +
        "(" + ID_COLUMN + ", " +  RECORD_ID_COLUMN + ", " + RECORD_TYPE_COLUMN + ", " + TAG_COLUMN
        + ") VALUES('" +
        UUID.randomUUID().toString() + "', '" + recordId + "', '" + recordType.getValue() + "', '" + value + "')",
      event -> future.complete(null));
    future.join();
  }

  public static void clearTags(Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "DELETE FROM " + PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TABLE_NAME,
      event -> future.complete(null));
    future.join();
  }

  public static List<String> getTags(Vertx vertx, RecordType recordType) {
    CompletableFuture<List<String>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(
      "SELECT " + TAG_COLUMN + " FROM " + PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TABLE_NAME +
        " WHERE " + RECORD_TYPE_COLUMN + "= '" + recordType.getValue()+"'",
      event -> future.complete(event.result().getRows().stream().map(row -> row.getString(TAG_COLUMN)).collect(Collectors.toList())));
    return future.join();
  }
}
