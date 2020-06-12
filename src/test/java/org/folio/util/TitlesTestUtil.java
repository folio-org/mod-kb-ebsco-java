package org.folio.util;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import static org.folio.repository.titles.TitlesTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.ID_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.NAME_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.TITLES_TABLE_NAME;
import static org.folio.rest.impl.TitlesTestData.STUB_MANAGED_TITLE_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_MANAGED_TITLE_ID_2;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.readFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;

import org.folio.db.RowSetUtils;
import org.folio.repository.DbUtil;
import org.folio.repository.SqlQueryHelper;
import org.folio.repository.titles.DbTitle;
import org.folio.rest.persist.PostgresClient;

public class TitlesTestUtil {

  private TitlesTestUtil() {}

  public static List<DbTitle> getTitles(Vertx vertx) {
    CompletableFuture<List<DbTitle>> future = new CompletableFuture<>();
    String query = DbUtil.prepareQuery(SqlQueryHelper.selectQuery(), titlesTestTable());
    PostgresClient.getInstance(vertx).select(query,
      event -> future.complete(RowSetUtils.mapItems(event.result(), TitlesTestUtil::mapDbTitle))
    );
    return future.join();
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
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/TEST_CUSTOMER_ID/titles/" + STUB_MANAGED_TITLE_ID),
        true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))));

    String stubResponseFile2 = "responses/rmapi/titles/get-title-by-id-2-response.json";
    stubFor(
      get(new UrlPathPattern(new RegexPattern("/rm/rmaccounts/TEST_CUSTOMER_ID/titles/" + STUB_MANAGED_TITLE_ID_2),
        true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile2))));
  }

  private static String titlesTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + TITLES_TABLE_NAME;
  }
}
