package org.folio.util;

import static org.apache.commons.lang3.StringUtils.join;

import static org.folio.repository.tag.TagTableConstants.ID_COLUMN;
import static org.folio.repository.tag.TagTableConstants.RECORD_ID_COLUMN;
import static org.folio.repository.tag.TagTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.repository.tag.TagTableConstants.TAG_COLUMN;
import static org.folio.repository.tag.TagTableConstants.TAG_FIELD_LIST;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import org.folio.db.RowSetUtils;
import org.folio.repository.RecordType;
import org.folio.repository.tag.DbTag;
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
      Tuple.of(UUID.randomUUID().toString(), recordId, recordType.getValue(), value),
      event -> future.complete(null));
    future.join();
  }

  public static List<DbTag> insertTags(List<DbTag> tags, Vertx vertx) {
    CompletableFuture<RowSet<Row>> future = new CompletableFuture<>();

    String insertStatement = "INSERT INTO " + tagTestTable() +
      "(" + TAG_FIELD_LIST + ") VALUES " + join(Collections.nCopies(tags.size(), "(?,?,?,?)"), ",") +
      " RETURNING " + ID_COLUMN;
    Tuple params = createParams(tags);

    PostgresClient.getInstance(vertx).select(insertStatement, params, ar -> {
      if (ar.succeeded()) {
        future.complete(ar.result());
      } else {
        future.completeExceptionally(ar.cause());
      }
    });

    return populateTagIds(tags, future.join());
  }

  private static List<DbTag> populateTagIds(List<DbTag> tags, RowSet<Row> keys) {
    List<DbTag> result = new ArrayList<>(tags.size());

    RowIterator<Row> iterator = keys.iterator();
    for (DbTag tag : tags) {
      result.add(tag.toBuilder()
        .id(iterator.next().getString(ID_COLUMN))
        .build());
    }

    return result;
  }

  public static List<String> getTags(Vertx vertx) {
    CompletableFuture<List<String>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(
      "SELECT " + TAG_COLUMN + " FROM " + tagTestTable(),
      event -> future.complete(RowSetUtils.mapItems(event.result(), row -> row.getString(TAG_COLUMN))));
    return future.join();
  }

  public static List<String> getTagsForRecordType(Vertx vertx, RecordType recordType) {
    CompletableFuture<List<String>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(
      "SELECT " + TAG_COLUMN + " FROM " + tagTestTable() + " " +
        "WHERE " + RECORD_TYPE_COLUMN + "= ?",
      Tuple.of(recordType.getValue()),
      event -> future.complete(RowSetUtils.mapItems(event.result(), row -> row.getString(TAG_COLUMN))));
    return future.join();
  }

  private static String tagTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TAGS_TABLE_NAME;
  }

  private static Tuple createParams(List<DbTag> tags) {
    Tuple params = Tuple.tuple();

    tags.forEach(tag -> {
      params.addValue(UUID.randomUUID().toString());
      params.addValue(tag.getRecordId());
      params.addValue(tag.getRecordType().getValue());
      params.addValue(tag.getValue());
    });

    return params;
  }
}
