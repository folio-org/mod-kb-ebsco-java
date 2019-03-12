package org.folio.rest.impl;

import static org.apache.commons.lang3.StringUtils.join;

import static org.folio.tag.repository.TagTableConstants.ID_COLUMN;
import static org.folio.tag.repository.TagTableConstants.RECORD_ID_COLUMN;
import static org.folio.tag.repository.TagTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.tag.repository.TagTableConstants.TABLE_NAME;
import static org.folio.tag.repository.TagTableConstants.TAG_COLUMN;
import static org.folio.tag.repository.TagTableConstants.TAG_FIELD_LIST;
import static org.folio.util.TestUtil.STUB_TENANT;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;

import org.folio.rest.persist.PostgresClient;
import org.folio.tag.RecordType;
import org.folio.tag.Tag;

public class TagsTestUtil {

  private TagsTestUtil() {
  }

  public static void insertTag(Vertx vertx, String recordId, final RecordType recordType, String value) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "INSERT INTO " + tagTestTable() +
        "(" + ID_COLUMN + ", " + RECORD_ID_COLUMN + ", " + RECORD_TYPE_COLUMN + ", " + TAG_COLUMN
        + ") VALUES('" +
        UUID.randomUUID().toString() + "', '" + recordId + "', '" + recordType.getValue() + "', '" + value + "')",
      event -> future.complete(null));
    future.join();
  }

  public static void insertTags(List<Tag> tags, Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    String insertStatement = "INSERT INTO " + tagTestTable() +
            "(" + TAG_FIELD_LIST + ") VALUES " + join(Collections.nCopies(tags.size(), "(?,?,?,?)"), ",");
    JsonArray params = createParams(tags);

    PostgresClient.getInstance(vertx).execute(insertStatement, params, event -> future.complete(null));

    future.join();
  }

  public static void clearTags(Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "DELETE FROM " + tagTestTable(),
      event -> future.complete(null));
    future.join();
  }

  public static List<String> getTags(Vertx vertx) {
    CompletableFuture<List<String>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(
      "SELECT " + TAG_COLUMN + " FROM " + tagTestTable(),
      event -> future.complete(event.result().getRows().stream().map(row -> row.getString(TAG_COLUMN)).collect(Collectors.toList())));
    return future.join();
  }

  public static List<String> getTagsForRecordType(Vertx vertx, RecordType recordType) {
    CompletableFuture<List<String>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(
      "SELECT " + TAG_COLUMN + " FROM " + tagTestTable() + " " +
        "WHERE " + RECORD_TYPE_COLUMN + "= '" + recordType.getValue()+"'",
      event -> future.complete(event.result().getRows().stream().map(row -> row.getString(TAG_COLUMN)).collect(Collectors.toList())));
    return future.join();
  }

  private static String tagTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TABLE_NAME;
  }

  private static JsonArray createParams(List<Tag> tags) {
    JsonArray params = new JsonArray();

    tags.forEach(tag -> {
      params.add(UUID.randomUUID().toString());
      params.add(tag.getRecordId());
      params.add(tag.getRecordType().getValue());
      params.add(tag.getValue());
    });

    return params;
  }
}
