package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.repository.holdings.HoldingsTableConstants.HOLDINGS_TABLE;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UcCredentialsTableConstants.UC_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UcSettingsTableConstants.UC_SETTINGS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsDetail;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.HoldingsTestUtil.saveHolding;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.KbTestUtil.clearDataFromTable;
import static org.folio.util.UcCredentialsTestUtil.setUpUcCredentials;
import static org.folio.util.UcSettingsTestUtil.saveUcSettings;
import static org.folio.util.UcSettingsTestUtil.stubSettings;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;

import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import org.folio.client.uc.UcApigeeEbscoClient;
import org.folio.client.uc.UcAuthEbscoClient;
import org.folio.client.uc.model.UcAuthToken;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.PackageCostPerUse;
import org.folio.rest.jaxrs.model.ResourceCostPerUse;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollection;
import org.folio.rest.jaxrs.model.TitleCostPerUse;
import org.folio.test.util.TestUtil;
import org.jeasy.random.EasyRandom;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RunWith(VertxUnitRunner.class)
public class EholdingsCostperuseImplTest extends WireMockTestBase {

  private final EasyRandom random = new EasyRandom();

  @Autowired
  private UcAuthEbscoClient authEbscoClient;
  @Autowired
  private UcApigeeEbscoClient apigeeEbscoClient;

  private String credentialsId;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    credentialsId = saveKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    saveUcSettings(stubSettings(credentialsId), vertx);
    setUpUcCredentials(vertx);

    ReflectionTestUtils.setField(authEbscoClient, "baseUrl", getWiremockUrl());
    ReflectionTestUtils.setField(apigeeEbscoClient, "baseUrl", getWiremockUrl());
    mockAuthToken();
  }

  @After
  public void after() {
    clearDataFromTable(vertx, UC_CREDENTIALS_TABLE_NAME);
    clearDataFromTable(vertx, UC_SETTINGS_TABLE_NAME);
    clearDataFromTable(vertx, HOLDINGS_TABLE);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnResourceCostPerUse() {
    int titleId = 356;
    int packageId = 473;
    String year = "2019";
    String platform = "all";
    String stubApigeeResponseFile = "responses/uc/titles/get-title-cost-per-use-response.json";
    mockSuccessfulTitleCostPerUse(titleId, packageId, stubApigeeResponseFile);

    String kbEbscoResponseFile = "responses/kb-ebsco/costperuse/resources/expected-resource-cost-per-use.json";
    ResourceCostPerUse expected = Json.decodeValue(readFile(kbEbscoResponseFile), ResourceCostPerUse.class);

    ResourceCostPerUse actual = getWithOk(resourceEndpoint(titleId, packageId, year, platform), JOHN_USER_ID_HEADER)
      .as(ResourceCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturnEmptyResourceCostPerUse() {
    int titleId = 356;
    int packageId = 473;
    String year = "2019";
    String stubApigeeResponseFile = "responses/uc/titles/get-empty-title-cost-per-use-response.json";
    mockSuccessfulTitleCostPerUse(titleId, packageId, stubApigeeResponseFile);

    String kbEbscoResponseFile = "responses/kb-ebsco/costperuse/resources/expected-empty-resource-cost-per-use.json";
    ResourceCostPerUse expected = Json.decodeValue(readFile(kbEbscoResponseFile), ResourceCostPerUse.class);

    ResourceCostPerUse actual = getWithOk(resourceEndpoint(titleId, packageId, year, null), JOHN_USER_ID_HEADER)
      .as(ResourceCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturnResourceCostPerUseWithEmptyNonPublisher() {
    int titleId = 356;
    int packageId = 473;
    String year = "2019";
    String platform = "all";
    String stubApigeeResponseFile = "responses/uc/titles/get-title-cost-per-use-response-with-empty-non-publisher.json";
    mockSuccessfulTitleCostPerUse(titleId, packageId, stubApigeeResponseFile);

    String kbEbscoResponseFile =
      "responses/kb-ebsco/costperuse/resources/expected-resource-cost-per-use-with-empty-non-publisher.json";
    ResourceCostPerUse expected = Json.decodeValue(readFile(kbEbscoResponseFile), ResourceCostPerUse.class);

    ResourceCostPerUse actual = getWithOk(resourceEndpoint(titleId, packageId, year, platform), JOHN_USER_ID_HEADER)
      .as(ResourceCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturn422OnGetResourceCpuWhenYearIsNull() {
    int titleId = 356;
    int packageId = 473;
    JsonapiError error = getWithStatus(resourceEndpoint(titleId, packageId, null, null), SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid fiscalYear");
  }

  @Test
  public void shouldReturn422OnGetResourceCpuWhenPlatformIsInvalid() {
    int titleId = 356;
    int packageId = 473;
    String year = "2019";
    String platform = "invalid";
    JsonapiError error = getWithStatus(resourceEndpoint(titleId, packageId, year, platform), SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid platform");
  }

  @Test
  public void shouldReturn400OnGetResourceCpuWhenApigeeFails() {
    int titleId = 356;
    int packageId = 473;
    String year = "2019";

    stubFor(get(urlPathMatching(String.format("/uc/costperuse/title/%s/%s", titleId, packageId)))
      .willReturn(aResponse().withStatus(SC_BAD_REQUEST).withBody("Random error message"))
    );

    JsonapiError error =
      getWithStatus(resourceEndpoint(titleId, packageId, year, null), SC_BAD_REQUEST, JOHN_USER_ID_HEADER)
        .as(JsonapiError.class);

    assertErrorContainsDetail(error, "Random error message");
  }

  @Test
  public void shouldReturnTitleCostPerUse() {
    int titleId = 1111111111;
    int packageId = 222222;
    final String year = "2019";
    final String platform = "all";
    final String stubRmapiResponseFile = "responses/rmapi/titles/get-custom-title-with-coverage-dates-asc.json";

    mockRmApiGetTitle(titleId, stubRmapiResponseFile);
    String stubApigeeGetTitleResponseFile = "responses/uc/titles/get-title-cost-per-use-response.json";
    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-title-packages-cost-per-use-response.json";
    mockSuccessfulTitleCostPerUse(titleId, packageId, stubApigeeGetTitleResponseFile);
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    String kbEbscoResponseFile = "responses/kb-ebsco/costperuse/titles/expected-title-cost-per-use.json";
    TitleCostPerUse expected = Json.decodeValue(readFile(kbEbscoResponseFile), TitleCostPerUse.class);

    TitleCostPerUse actual = getWithOk(titleEndpoint(titleId, year, platform), JOHN_USER_ID_HEADER)
      .as(TitleCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturnTitleCostPerUseWithManagedEmbargoPeriod() {
    int titleId = 1111111111;
    int packageId = 222222;
    final String year = "2019";
    final String platform = "nonPublisher";
    final String stubRmapiResponseFile = "responses/rmapi/titles/get-custom-title-with-managed-embargo.json";

    mockRmApiGetTitle(titleId, stubRmapiResponseFile);
    String stubApigeeGetTitleResponseFile = "responses/uc/titles/get-empty-title-cost-per-use-response.json";
    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-title-packages-cost-per-use-response.json";
    mockSuccessfulTitleCostPerUse(titleId, packageId, stubApigeeGetTitleResponseFile);
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    String kbEbscoResponseFile = "responses/kb-ebsco/costperuse/titles/expected-title-cost-per-use-with-no-usage.json";
    TitleCostPerUse expected = Json.decodeValue(readFile(kbEbscoResponseFile), TitleCostPerUse.class);

    TitleCostPerUse actual = getWithOk(titleEndpoint(titleId, year, platform), JOHN_USER_ID_HEADER)
      .as(TitleCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturnEmptyTitleCostPerUseWhenNoCostPerUseDataAvailable() {
    int titleId = 1111111111;
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";
    final String stubRmapiResponseFile = "responses/rmapi/titles/get-custom-title-with-coverage-dates-asc.json";

    mockRmApiGetTitle(titleId, stubRmapiResponseFile);
    String stubApigeeGetTitleResponseFile = "responses/uc/titles/get-empty-title-cost-per-use-response.json";
    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-title-packages-cost-per-use-response.json";
    mockSuccessfulTitleCostPerUse(titleId, packageId, stubApigeeGetTitleResponseFile);
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    String kbEbscoResponseFile = "responses/kb-ebsco/costperuse/titles/expected-title-cost-per-use-with-no-usage.json";
    TitleCostPerUse expected = Json.decodeValue(readFile(kbEbscoResponseFile), TitleCostPerUse.class);

    TitleCostPerUse actual = getWithOk(titleEndpoint(titleId, year, platform), JOHN_USER_ID_HEADER)
      .as(TitleCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturnEmptyTitleCostPerUseWhenNoSelectedResources() {
    int titleId = 1111111111;
    String year = "2019";
    String platform = "all";

    String stubRmapiResponseFile = "responses/rmapi/titles/get-custom-title-with-no-selected-resources.json";
    mockRmApiGetTitle(titleId, stubRmapiResponseFile);

    String kbEbscoResponseFile = "responses/kb-ebsco/costperuse/titles/expected-empty-title-cost-per-use.json";
    TitleCostPerUse expected = Json.decodeValue(readFile(kbEbscoResponseFile), TitleCostPerUse.class);

    TitleCostPerUse actual = getWithOk(titleEndpoint(titleId, year, platform), JOHN_USER_ID_HEADER)
      .as(TitleCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturn422OnGetTitleCpuWhenYearIsNull() {
    int titleId = 356;
    JsonapiError error = getWithStatus(titleEndpoint(titleId, null, null), SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid fiscalYear");
  }

  @Test
  public void shouldReturn422OnGetTitleCpuWhenPlatformIsInvalid() {
    int titleId = 356;
    String year = "2019";
    String platform = "invalid";
    JsonapiError error = getWithStatus(titleEndpoint(titleId, year, platform), SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid platform");
  }

  @Test
  public void shouldReturnPackageCostPerUse() {
    int packageId = 222222;
    String year = "2019";
    String platform = "all";

    String stubApigeeGetPackageResponseFile = "responses/uc/packages/get-package-cost-per-use-response.json";
    mockSuccessfulPackageCostPerUse(packageId, stubApigeeGetPackageResponseFile);

    String kbEbscoResponseFile = "responses/kb-ebsco/costperuse/packages/expected-package-cost-per-use.json";
    PackageCostPerUse expected = Json.decodeValue(readFile(kbEbscoResponseFile), PackageCostPerUse.class);

    PackageCostPerUse actual = getWithOk(packageEndpoint(packageId, year, platform), JOHN_USER_ID_HEADER)
      .as(PackageCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturnPackageCostPerUseWhenPackageCostIsEmpty() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "all";

    var holding1 = new DbHoldingInfo(1, packageId, 1, "Ionicis tormentos accelerare!", "Sunt hydraes", "Book");
    var holding2 = new DbHoldingInfo(2, packageId, 1, "Vortex, plasmator, et lixa.", "Est germanus byssus", "Book");
    saveHolding(credentialsId, holding1, OffsetDateTime.now(), vertx);
    saveHolding(credentialsId, holding2, OffsetDateTime.now(), vertx);

    String stubApigeeGetPackageResponseFile =
      "responses/uc/packages/get-package-cost-per-use-with-empty-cost-response.json";
    mockSuccessfulPackageCostPerUse(packageId, stubApigeeGetPackageResponseFile);
    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-title-packages-cost-per-use-for-package-response.json";
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    String kbEbscoResponseFile =
      "responses/kb-ebsco/costperuse/packages/expected-package-cost-per-use-when-cost-is-empty.json";
    PackageCostPerUse expected = Json.decodeValue(readFile(kbEbscoResponseFile), PackageCostPerUse.class);

    PackageCostPerUse actual = getWithOk(packageEndpoint(packageId, year, platform), JOHN_USER_ID_HEADER)
      .as(PackageCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturn422OnGetPackageCpuWhenYearIsNull() {
    int packageId = 222222;
    JsonapiError error = getWithStatus(packageEndpoint(packageId, null, null), SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid fiscalYear");
  }

  @Test
  public void shouldReturn422OnGetPackageCpuWhenPlatformIsInvalid() {
    int packageId = 222222;
    String year = "2019";
    String platform = "invalid";
    JsonapiError error = getWithStatus(packageEndpoint(packageId, year, platform), SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid platform");
  }

  @Test
  public void shouldReturn400OnGetPackageCpuWhenApigeeFails() {
    int packageId = 222222;
    String year = "2019";

    stubFor(get(urlPathMatching(String.format("/uc/costperuse/package/%s", packageId)))
      .willReturn(aResponse().withStatus(SC_BAD_REQUEST).withBody("Random error message"))
    );

    JsonapiError error = getWithStatus(packageEndpoint(packageId, year, null), SC_BAD_REQUEST, JOHN_USER_ID_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsDetail(error, "Random error message");
  }

  @Test
  public void shouldReturnResourcesCostPerUseCollection() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "all";

    for (int i = 1; i <= 20; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    String stubApigeeGetPackageResponseFile =
      "responses/uc/packages/get-package-cost-per-use-with-empty-cost-response.json";
    mockSuccessfulPackageCostPerUse(packageId, stubApigeeGetPackageResponseFile);
    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-multiply-title-packages-cost-per-use-for-package-response.json";
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    ResourceCostPerUseCollection
      actual = getWithOk(packageResourcesEndpoint(packageId, year, platform, null, null), JOHN_USER_ID_HEADER)
      .as(ResourceCostPerUseCollection.class);

    assertThat(actual, notNullValue());
    assertThat(actual.getMeta().getTotalResults(), equalTo(20));
    assertThat(actual.getData(), hasSize(20));
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().getFirst().getAttributes(), hasProperty("usage", equalTo(2)));
    assertThat(actual.getData().getFirst().getAttributes(), hasProperty("percent", equalTo(2.0 / 36 * 100)));
  }

  @Test
  public void shouldReturnResourcesCostPerUseCollectionWithPagination() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";
    final var page = "2";
    final var size = "15";

    for (int i = 1; i <= 20; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    String stubApigeeGetPackageResponseFile =
      "responses/uc/packages/get-package-cost-per-use-with-empty-cost-response.json";
    mockSuccessfulPackageCostPerUse(packageId, stubApigeeGetPackageResponseFile);
    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-multiply-title-packages-cost-per-use-for-package-response.json";
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    ResourceCostPerUseCollection actual =
      getWithOk(packageResourcesEndpoint(packageId, year, platform, page, size), JOHN_USER_ID_HEADER)
        .as(ResourceCostPerUseCollection.class);

    assertThat(actual, notNullValue());
    assertThat(actual.getMeta().getTotalResults(), equalTo(20));
    assertThat(actual.getData(), hasSize(5));
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().getFirst().getAttributes(), hasProperty("usage", equalTo(1)));
  }

  @Test
  public void shouldReturnResourcesCostPerUseCollectionWithSortByUsageAsc() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";
    final String sort = "usage";
    final String order = "asc";

    for (int i = 1; i <= 3; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    String stubApigeeGetPackageResponseFile =
      "responses/uc/packages/get-package-cost-per-use-with-empty-cost-response.json";
    mockSuccessfulPackageCostPerUse(packageId, stubApigeeGetPackageResponseFile);
    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-different-title-packages-cost-per-use-for-package-response.json";
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    var actual =
      getWithOk(packageResourcesEndpoint(packageId, year, platform, null, null, sort, order), JOHN_USER_ID_HEADER)
        .as(ResourceCostPerUseCollection.class);

    assertThat(actual, notNullValue());
    assertThat(actual.getMeta().getTotalResults(), equalTo(3));
    assertThat(actual.getData(), hasSize(3));
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().get(0).getAttributes(), hasProperty("usage", equalTo(1)));
    assertThat(actual.getData().get(2).getAttributes(), hasProperty("usage", equalTo(10)));
  }

  @Test
  public void shouldReturnResourcesCostPerUseCollectionWithSortByUsageDesc() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";
    final String sort = "usage";
    final String order = "desc";

    for (int i = 1; i <= 3; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    String stubApigeeGetPackageResponseFile =
      "responses/uc/packages/get-package-cost-per-use-with-empty-cost-response.json";
    mockSuccessfulPackageCostPerUse(packageId, stubApigeeGetPackageResponseFile);
    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-different-title-packages-cost-per-use-for-package-response.json";
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    var actual =
      getWithOk(packageResourcesEndpoint(packageId, year, platform, null, null, sort, order), JOHN_USER_ID_HEADER)
        .as(ResourceCostPerUseCollection.class);

    assertThat(actual, notNullValue());
    assertThat(actual.getMeta().getTotalResults(), equalTo(3));
    assertThat(actual.getData(), hasSize(3));
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().get(0).getAttributes(), hasProperty("usage", equalTo(10)));
    assertThat(actual.getData().get(2).getAttributes(), hasProperty("usage", equalTo(1)));
  }

  @Test
  public void shouldReturnResourcesCostPerUseCollectionWithSortByCost() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";
    final String sort = "cost";

    for (int i = 1; i <= 3; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    String stubApigeeGetPackageResponseFile =
      "responses/uc/packages/get-package-cost-per-use-with-empty-cost-response.json";
    mockSuccessfulPackageCostPerUse(packageId, stubApigeeGetPackageResponseFile);
    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-different-title-packages-cost-per-use-for-package-response.json";
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    ResourceCostPerUseCollection
      actual = getWithOk(packageResourcesEndpoint(packageId, year, platform, null, null, sort, null),
      JOHN_USER_ID_HEADER)
      .as(ResourceCostPerUseCollection.class);

    assertThat(actual, notNullValue());
    assertThat(actual.getMeta().getTotalResults(), equalTo(3));
    assertThat(actual.getData(), hasSize(3));
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().get(0).getAttributes(), hasProperty("cost", nullValue()));
    assertThat(actual.getData().get(2).getAttributes(), hasProperty("cost", equalTo(200.0)));
  }

  @Test
  public void shouldReturnResourcesCostPerUseCollectionWithSortByCostPerUse() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";
    final String sort = "costperuse";

    for (int i = 1; i <= 3; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    String stubApigeeGetPackageResponseFile =
      "responses/uc/packages/get-package-cost-per-use-with-empty-cost-response.json";
    mockSuccessfulPackageCostPerUse(packageId, stubApigeeGetPackageResponseFile);
    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-different-title-packages-cost-per-use-for-package-response.json";
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    ResourceCostPerUseCollection
      actual = getWithOk(packageResourcesEndpoint(packageId, year, platform, null, null, sort, null),
      JOHN_USER_ID_HEADER)
      .as(ResourceCostPerUseCollection.class);

    assertThat(actual, notNullValue());
    assertThat(actual.getMeta().getTotalResults(), equalTo(3));
    assertThat(actual.getData(), hasSize(3));
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().get(0).getAttributes(), hasProperty("costPerUse", nullValue()));
    assertThat(actual.getData().get(2).getAttributes(), hasProperty("costPerUse", equalTo(50.0)));
  }

  @Test
  public void shouldReturnResourcesCostPerUseCollectionWithSortByPercent() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";
    final String sort = "percent";

    for (int i = 1; i <= 3; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    String stubApigeeGetPackageResponseFile =
      "responses/uc/packages/get-package-cost-per-use-with-empty-cost-response.json";
    mockSuccessfulPackageCostPerUse(packageId, stubApigeeGetPackageResponseFile);
    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-different-title-packages-cost-per-use-for-package-response.json";
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    ResourceCostPerUseCollection
      actual = getWithOk(packageResourcesEndpoint(packageId, year, platform, null, null, sort, null),
      JOHN_USER_ID_HEADER)
      .as(ResourceCostPerUseCollection.class);

    assertThat(actual, notNullValue());
    assertThat(actual.getMeta().getTotalResults(), equalTo(3));
    assertThat(actual.getData(), hasSize(3));
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().get(0).getAttributes(), hasProperty("percent", equalTo(1.0 / 26 * 100)));
    assertThat(actual.getData().get(2).getAttributes(), hasProperty("percent", equalTo(10.0 / 26 * 100)));
  }

  @Test
  public void shouldReturnResourcesCostPerUseCollectionSortedByNameWhenSortingByEqualsValues() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";
    final String sort = "type";

    for (int i = 1; i <= 3; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i, String.valueOf(i)), OffsetDateTime.now(), vertx);
    }

    String stubApigeeGetPackageResponseFile =
      "responses/uc/packages/get-package-cost-per-use-with-empty-cost-response.json";
    mockSuccessfulPackageCostPerUse(packageId, stubApigeeGetPackageResponseFile);
    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-different-title-packages-cost-per-use-for-package-response.json";
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    ResourceCostPerUseCollection
      actual = getWithOk(packageResourcesEndpoint(packageId, year, platform, null, null, sort, null),
      JOHN_USER_ID_HEADER)
      .as(ResourceCostPerUseCollection.class);

    assertThat(actual, notNullValue());
    assertThat(actual.getMeta().getTotalResults(), equalTo(3));
    assertThat(actual.getData(), hasSize(3));
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().get(0).getAttributes(), hasProperty("name", equalTo("1")));
    assertThat(actual.getData().get(2).getAttributes(), hasProperty("name", equalTo("3")));
  }

  @Test
  public void shouldReturn422OnGetPackageResourcesCpuWhenYearIsNull() {
    int packageId = 222222;
    JsonapiError error =
      getWithStatus(packageResourcesEndpoint(packageId, null, null, null, null), SC_UNPROCESSABLE_ENTITY)
        .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid fiscalYear");
  }

  @Test
  public void shouldReturn422OnGetPackageResourcesCpuWhenPlatformIsInvalid() {
    int packageId = 222222;
    String year = "2019";
    String platform = "invalid";
    JsonapiError error =
      getWithStatus(packageResourcesEndpoint(packageId, year, platform, null, null), SC_UNPROCESSABLE_ENTITY)
        .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid platform");
  }

  @Test
  public void shouldReturn422OnGetPackageResourcesCpuWhenSortIsInvalid() {
    int packageId = 222222;
    String year = "2019";
    String platform = "all";
    String sort = "invalid";
    JsonapiError error =
      getWithStatus(packageResourcesEndpoint(packageId, year, platform, null, null, sort, null),
        SC_UNPROCESSABLE_ENTITY)
        .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid sort");
  }

  @Test
  public void shouldReturn400OnGetPackageResourcesCpuWhenApigeeFails() {
    final int packageId = 222222;
    final String year = "2019";

    saveHolding(credentialsId, generateHolding(packageId, 1), OffsetDateTime.now(), vertx);

    String stubApigeeGetPackageResponseFile =
      "responses/uc/packages/get-package-cost-per-use-with-empty-cost-response.json";
    mockSuccessfulPackageCostPerUse(packageId, stubApigeeGetPackageResponseFile);

    stubFor(post(urlPathMatching("/uc/costperuse/titles"))
      .willReturn(aResponse().withStatus(SC_BAD_REQUEST).withBody("Random error message"))
    );

    JsonapiError error =
      getWithStatus(packageResourcesEndpoint(packageId, year, null, null, null), SC_BAD_REQUEST, JOHN_USER_ID_HEADER)
        .as(JsonapiError.class);

    assertErrorContainsDetail(error, "Random error message");
  }

  private void mockRmApiGetTitle(int titleId, String stubRmapiResponseFile) {
    stubFor(get(urlPathMatching("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + titleId))
      .willReturn(aResponse().withBody(readFile(stubRmapiResponseFile)))
    );
  }

  private void mockSuccessfulTitleCostPerUse(int titleId, int packageId, String filePath) {
    stubFor(get(urlPathMatching(String.format("/uc/costperuse/title/%s/%s", titleId, packageId)))
      .willReturn(aResponse().withStatus(SC_OK).withBody(readFile(filePath)))
    );
  }

  private void mockSuccessfulPackageCostPerUse(int packageId, String filePath) {
    stubFor(get(urlPathMatching(String.format("/uc/costperuse/package/%s", packageId)))
      .willReturn(aResponse().withStatus(SC_OK).withBody(readFile(filePath)))
    );
  }

  private void mockSuccessfulTitlePackageCostPerUse(String filePath) {
    stubFor(post(urlPathMatching("/uc/costperuse/titles"))
      .willReturn(aResponse().withStatus(SC_OK).withBody(readFile(filePath)))
    );
  }

  private void mockAuthToken() {
    UcAuthToken stubToken = new UcAuthToken("access_token", "Bearer", 3600L, "openid");
    stubFor(post(urlPathMatching("/oauth-proxy/token"))
      .willReturn(aResponse().withStatus(SC_OK).withBody(Json.encode(stubToken)))
    );
  }

  private String readFile(String filePath) {
    try {
      return TestUtil.readFile(filePath);
    } catch (IOException | URISyntaxException e) {
      Assert.fail(e.getMessage());
      return null;
    }
  }

  private String resourceEndpoint(int titleId, int packageId, String year, String platform) {
    String baseUrl = String.format("eholdings/resources/1-%s-%s/costperuse", packageId, titleId);
    StringBuilder paramsSb = getEndpointParams(year, platform);
    return !paramsSb.isEmpty()
           ? baseUrl + "?" + paramsSb
           : baseUrl;
  }

  private String titleEndpoint(int titleId, String year, String platform) {
    String baseUrl = String.format("eholdings/titles/%s/costperuse", titleId);
    StringBuilder paramsSb = getEndpointParams(year, platform);
    return !paramsSb.isEmpty()
           ? baseUrl + "?" + paramsSb
           : baseUrl;
  }

  private String packageEndpoint(int packageId, String year, String platform) {
    String baseUrl = String.format("eholdings/packages/1-%s/costperuse", packageId);
    StringBuilder paramsSb = getEndpointParams(year, platform);
    return !paramsSb.isEmpty()
           ? baseUrl + "?" + paramsSb
           : baseUrl;
  }

  private String packageResourcesEndpoint(int packageId, String year, String platform, String page, String size) {
    return packageResourcesEndpoint(packageId, year, platform, page, size, null, null);
  }

  private String packageResourcesEndpoint(int packageId, String year, String platform, String page, String size,
                                          String sort, String order) {
    String baseUrl = String.format("eholdings/packages/1-%s/resources/costperuse", packageId);
    StringBuilder paramsSb = getEndpointParams(year, platform, page, size, sort, order);
    return !paramsSb.isEmpty()
           ? baseUrl + "?" + paramsSb
           : baseUrl;
  }

  private StringBuilder getEndpointParams(String year, String platform) {
    return getEndpointParams(year, platform, null, null, null, null);
  }

  private StringBuilder getEndpointParams(String year, String platform, String page, String size, String sort,
                                          String order) {
    StringBuilder paramsSb = new StringBuilder();
    if (year != null) {
      paramsSb.append("fiscalYear=").append(year);
    }
    addParam(platform, paramsSb, "platform=");
    addParam(page, paramsSb, "page=");
    addParam(size, paramsSb, "count=");
    addParam(sort, paramsSb, "sort=");
    addParam(order, paramsSb, "order=");
    return paramsSb;
  }

  private void addParam(String platform, StringBuilder paramsSb, String s) {
    if (platform != null) {
      if (!paramsSb.isEmpty()) {
        paramsSb.append("&");
      }
      paramsSb.append(s).append(platform);
    }
  }

  private DbHoldingInfo generateHolding(int packageId, int titleId) {
    return DbHoldingInfo.builder()
      .packageId(packageId)
      .titleId(titleId)
      .publicationTitle(random.nextObject(String.class))
      .publisherName(random.nextObject(String.class))
      .resourceType("Book")
      .vendorId(1)
      .build();
  }

  private DbHoldingInfo generateHolding(int packageId, int titleId, String titleName) {
    return DbHoldingInfo.builder()
      .packageId(packageId)
      .titleId(titleId)
      .publicationTitle(titleName)
      .publisherName(random.nextObject(String.class))
      .resourceType("Book")
      .vendorId(1)
      .build();
  }
}
