package org.folio.rest.impl;

import static org.folio.util.TestUtil.STUB_TENANT;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.persist.PostgresClient;
import org.folio.tag.RecordType;
import org.folio.tag.repository.TagTableConstants;

import io.vertx.core.Vertx;

public class TagsTestUtil {
  private TagsTestUtil() { }

  public static void insertTag(Vertx vertx, String recordId, final RecordType recordType, String value) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "INSERT INTO " + PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TagTableConstants.TABLE_NAME +
        "(" + TagTableConstants.RECORD_ID_COLUMN + ", " + TagTableConstants.RECORD_TYPE_COLUMN + ", " + TagTableConstants.TAG_COLUMN
        + ") VALUES('" +
        recordId + "', '" + recordType.getValue() + "', '" + value + "')",
      event -> future.complete(null));
    future.join();
  }

  public static void clearTags(Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "DELETE FROM " + PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TagTableConstants.TABLE_NAME,
      event -> future.complete(null));
    future.join();
  }

}
