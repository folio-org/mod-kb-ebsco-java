package org.folio.util;

import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.SqlQueryHelper.whereQuery;
import static org.folio.repository.tag.TagTableConstants.ID_COLUMN;
import static org.folio.repository.tag.TagTableConstants.RECORD_ID_COLUMN;
import static org.folio.repository.tag.TagTableConstants.RECORD_TYPE_COLUMN;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.repository.tag.TagTableConstants.TAG_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.ArrayList;
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

  public static void saveTag(Vertx vertx, String recordId, RecordType recordType, String value) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    String query = prepareQuery(insertQuery(ID_COLUMN, RECORD_ID_COLUMN, RECORD_TYPE_COLUMN, TAG_COLUMN), tagTestTable());
    Tuple params = Tuple.of(UUID.randomUUID(), recordId, recordType.getValue(), value);
    PostgresClient.getInstance(vertx).execute(query, params, event -> future.complete(null));
    future.join();
  }

  public static List<DbTag> saveTags(List<DbTag> tags, Vertx vertx) {
    CompletableFuture<RowSet<Row>> future = new CompletableFuture<>();

    String query = prepareQuery(
      insertQuery(tags.size(), ID_COLUMN, RECORD_ID_COLUMN, RECORD_TYPE_COLUMN, TAG_COLUMN),
      tagTestTable()
    );
    Tuple params = createParams(tags);

    PostgresClient.getInstance(vertx).select(query, params, ar -> {
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
        .id(iterator.next().getUUID(ID_COLUMN))
        .build());
    }

    return result;
  }

  public static List<String> getTags(Vertx vertx) {
    CompletableFuture<List<String>> future = new CompletableFuture<>();
    String query = prepareQuery(selectQuery(TAG_COLUMN), tagTestTable());
    PostgresClient.getInstance(vertx).select(query,
      event -> future.complete(RowSetUtils.mapItems(event.result(), row -> row.getString(TAG_COLUMN)))
    );
    return future.join();
  }

  public static List<String> getTagsForRecordType(Vertx vertx, RecordType recordType) {
    CompletableFuture<List<String>> future = new CompletableFuture<>();
    String query = prepareQuery(selectQuery(TAG_COLUMN) + " " + whereQuery(RECORD_TYPE_COLUMN), tagTestTable());
    Tuple params = Tuple.of(recordType.getValue());
    PostgresClient.getInstance(vertx).select(query, params,
      event -> future.complete(RowSetUtils.mapItems(event.result(), row -> row.getString(TAG_COLUMN))));
    return future.join();
  }

  private static String tagTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TAGS_TABLE_NAME;
  }

  private static Tuple createParams(List<DbTag> tags) {
    Tuple params = Tuple.tuple();

    tags.forEach(tag -> {
      params.addValue(UUID.randomUUID());
      params.addValue(tag.getRecordId());
      params.addValue(tag.getRecordType().getValue());
      params.addValue(tag.getValue());
    });

    return params;
  }
}
