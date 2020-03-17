package org.folio.util;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.resources.ResourceTableConstants.ID_COLUMN;
import static org.folio.repository.resources.ResourceTableConstants.NAME_COLUMN;
import static org.folio.repository.resources.ResourceTableConstants.RESOURCES_TABLE_NAME;
import static org.folio.rest.impl.TitlesTestData.STUB_MANAGED_TITLE_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_MANAGED_TITLE_ID_2;
import static org.folio.test.util.TestUtil.STUB_TENANT;
import static org.folio.test.util.TestUtil.readFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import lombok.Builder;
import lombok.Value;

import org.folio.rest.persist.PostgresClient;

public class ResourcesTestUtil {

  public ResourcesTestUtil() {
  }

  public static List<ResourcesTestUtil.DbResources> getResources(Vertx vertx) {
    CompletableFuture<List<ResourcesTestUtil.DbResources>> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).select(
      "SELECT " + NAME_COLUMN + ", " + ID_COLUMN + " FROM " + resourceTestTable(),
      event -> future.complete(mapItems(event.result().getRows(),
        row ->
          ResourcesTestUtil.DbResources.builder()
            .id(row.getString(ID_COLUMN))
            .name(row.getString(NAME_COLUMN))
            .build()
      )));
    return future.join();
  }

  public static void addResource(Vertx vertx, DbResources dbResource) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PostgresClient.getInstance(vertx).execute(
      "INSERT INTO " + resourceTestTable() +
        "(" + ID_COLUMN + ", " + NAME_COLUMN + ") VALUES(?,?)",
      new JsonArray(Arrays.asList(dbResource.getId(), dbResource.getName() )),
      event -> future.complete(null));
    future.join();
  }

  private static String resourceTestTable() {
    return PostgresClient.convertToPsqlStandard(STUB_TENANT) + "." + RESOURCES_TABLE_NAME;
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

  @Value
  @Builder(toBuilder = true)
  public static class DbResources {
    private String id;
    private String name;
  }
}
