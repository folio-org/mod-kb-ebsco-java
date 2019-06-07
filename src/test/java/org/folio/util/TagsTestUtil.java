package org.folio.util;

import static org.apache.commons.lang3.StringUtils.join;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.tag.TagTableConstants.ID_COLUMN;
import static org.folio.repository.tag.TagTableConstants.RECORD_ID_COLUMN;
import static org.folio.repository.tag.TagTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.repository.tag.TagTableConstants.TAG_COLUMN;
import static org.folio.repository.tag.TagTableConstants.TAG_FIELD_LIST;
import static org.folio.util.TestUtil.STUB_TENANT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;

import org.folio.repository.RecordType;
import org.folio.repository.tag.Tag;
import org.folio.rest.persist.PostgresClient;

public class TagsTestUtil {

  private TagsTestUtil() {
  }

  public static void insertTag(Vertx vertx, String recordId, final RecordType recordType, String value) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "INSERT INTO " + tagTestTable() +
        "(" + ID_COLUMN + ", " + RECORD_ID_COLUMN + ", " + RECORD_TYPE_COLUMN + ", " + TAG_COLUMN
        + ") VALUES(?,?,?,?)",
      new JsonArray(Arrays.asList(UUID.randomUUID().toString(), recordId, recordType.getValue(), value)),
      event -> future.complete(null));
    future.join();
  }

  public static List<Tag> insertTags(List<Tag> tags, Vertx vertx) {
    CompletableFuture<ResultSet> future = new CompletableFuture<>();

    String insertStatement = "INSERT INTO " + tagTestTable() +
            "(" + TAG_FIELD_LIST + ") VALUES " + join(Collections.nCopies(tags.size(), "(?,?,?,?)"), ",") +
            " RETURNING " + ID_COLUMN;
    JsonArray params = createParams(tags);

    PostgresClient.getInstance(vertx).select(insertStatement, params, ar -> {
      if (ar.succeeded()) {
        future.complete(ar.result());
      } else {
        future.completeExceptionally(ar.cause());
      }
    });

    return populateTagIds(tags, future.join().getRows());
  }

  private static List<Tag> populateTagIds(List<Tag> tags, List<JsonObject> keys) {
    List<Tag> result = new ArrayList<>(tags.size());

    for (int i = 0; i < tags.size(); i++) {
      result.add(tags.get(i).toBuilder()
        .id(keys.get(i).getString(ID_COLUMN))
        .build());
    }

    return result;
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
      event -> future.complete(mapItems(event.result().getRows(), row -> row.getString(TAG_COLUMN))));
    return future.join();
  }

  public static List<String> getTagsForRecordType(Vertx vertx, RecordType recordType) {
    CompletableFuture<List<String>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(
      "SELECT " + TAG_COLUMN + " FROM " + tagTestTable() + " " +
        "WHERE " + RECORD_TYPE_COLUMN + "= ?",
      new JsonArray(Collections.singletonList(recordType.getValue())),
      event -> future.complete(mapItems(event.result().getRows(), row -> row.getString(TAG_COLUMN))));
    return future.join();
  }

  private static String tagTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TAGS_TABLE_NAME;
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
