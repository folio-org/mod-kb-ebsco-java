package org.folio.util;

import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.titles.TitlesTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.ID_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.NAME_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.TITLES_TABLE_NAME;
import static org.folio.util.TestUtil.STUB_TENANT;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.experimental.UtilityClass;
import org.folio.db.RowSetUtils;
import org.folio.repository.SqlQueryHelper;
import org.folio.repository.titles.DbTitle;
import org.folio.rest.persist.PostgresClient;

@UtilityClass
public final class TitlesTestUtil {

  public static List<DbTitle> getTitles(Vertx vertx) {
    CompletableFuture<List<DbTitle>> future = new CompletableFuture<>();
    String query = prepareQuery(SqlQueryHelper.selectQuery(), titlesTestTable());
    PostgresClient.getInstance(vertx, STUB_TENANT)
      .select(query, event -> future.complete(RowSetUtils.mapItems(event.result(), TitlesTestUtil::mapDbTitle)));
    return future.join();
  }

  public static void saveTitle(DbTitle dbTitle, Vertx vertx) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    String query = prepareQuery(insertQuery(ID_COLUMN, CREDENTIALS_ID_COLUMN, NAME_COLUMN), titlesTestTable());
    Tuple params = Tuple.of(dbTitle.getId(), dbTitle.getCredentialsId(), dbTitle.getName());

    PostgresClient.getInstance(vertx, STUB_TENANT).execute(query, params, event -> future.complete(null));

    future.join();
  }

  public static DbTitle buildTitle(int id, String credentialsId, String name) {
    return buildResource(id, toUUID(credentialsId), name);
  }

  private static DbTitle buildResource(int id, UUID credentialsId, String name) {
    return DbTitle.builder().id((long) id).credentialsId(credentialsId).name(name).build();
  }

  private static DbTitle mapDbTitle(Row row) {
    return DbTitle.builder()
      .id(Long.valueOf(row.getString(ID_COLUMN)))
      .credentialsId(row.getUUID(CREDENTIALS_ID_COLUMN))
      .name(row.getString(NAME_COLUMN))
      .build();
  }

  private static String titlesTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TITLES_TABLE_NAME;
  }
}
