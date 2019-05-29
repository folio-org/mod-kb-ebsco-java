package org.folio.util;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.titles.TitlesTableConstants.ID_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.NAME_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.TITLES_TABLE_NAME;
import static org.folio.util.TestUtil.STUB_TENANT;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import lombok.Builder;
import lombok.Value;

import org.folio.rest.persist.PostgresClient;

public class TitlesTestUtil {
  private TitlesTestUtil() {}

  public static List<TitlesTestUtil.DbTitle> getTitles(Vertx vertx) {
    CompletableFuture<List<TitlesTestUtil.DbTitle>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(
      "SELECT " + NAME_COLUMN + ", " + ID_COLUMN + " FROM " + titlesTestTable(),
      event -> future.complete(mapItems(event.result().getRows(),
        row ->
          TitlesTestUtil.DbTitle.builder()
            .id(row.getString(ID_COLUMN))
            .name(row.getString(NAME_COLUMN))
            .build()
      )));
    return future.join();
  }

  private static String titlesTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TITLES_TABLE_NAME;
  }

  @Value
  @Builder(toBuilder = true)
  public static class DbTitle {
    private String id;
    private String name;
  }
}
