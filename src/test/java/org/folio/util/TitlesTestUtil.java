package org.folio.util;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.titles.TitlesTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.ID_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.NAME_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.TITLES_TABLE_NAME;
import static org.folio.rest.impl.TitlesTestData.STUB_MANAGED_TITLE_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_MANAGED_TITLE_ID_2;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.readFile;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.folio.db.RowSetUtils;
import org.folio.repository.SqlQueryHelper;
import org.folio.repository.titles.DbTitle;
import org.folio.rest.persist.PostgresClient;

public final class TitlesTestUtil {

  private TitlesTestUtil() { }

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

  public static DbTitle buildTitle(String id, String credentialsId, String name) {
    return buildResource(Long.valueOf(id), toUUID(credentialsId), name);
  }

  private static DbTitle buildResource(Long id, UUID credentialsId, String name) {
    return DbTitle.builder().id(id).credentialsId(credentialsId).name(name).build();
  }

  private static DbTitle mapDbTitle(Row row) {
    return DbTitle.builder()
      .id(Long.valueOf(row.getString(ID_COLUMN)))
      .credentialsId(row.getUUID(CREDENTIALS_ID_COLUMN))
      .name(row.getString(NAME_COLUMN))
      .build();
  }

  public static void mockGetTitles() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/titles/get-title-by-id-response.json";
    stubFor(get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/TEST_CUSTOMER_ID/titles/" + STUB_MANAGED_TITLE_ID),
      true)).willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile))));

    String stubResponseFile2 = "responses/rmapi/titles/get-title-by-id-2-response.json";
    stubFor(get(
      new UrlPathPattern(new RegexPattern("/rm/rmaccounts/TEST_CUSTOMER_ID/titles/" + STUB_MANAGED_TITLE_ID_2),
        true)).willReturn(new ResponseDefinitionBuilder().withBody(readFile(stubResponseFile2))));
  }

  private static String titlesTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TITLES_TABLE_NAME;
  }
}
