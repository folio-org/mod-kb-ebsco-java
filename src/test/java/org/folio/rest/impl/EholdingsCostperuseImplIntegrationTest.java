package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static org.folio.HttpStatus.SC_BAD_REQUEST;
import static org.folio.HttpStatus.SC_OK;
import static org.folio.HttpStatus.SC_UNPROCESSABLE_CONTENT;
import static org.folio.repository.holdings.HoldingsTableConstants.HOLDINGS_TABLE;
import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UcCredentialsTableConstants.UC_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UcSettingsTableConstants.UC_SETTINGS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsDetail;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.HoldingsTestUtil.saveHolding;
import static org.folio.util.KbCredentialsTestUtil.setupDefaultKbConfiguration;
import static org.folio.util.TestUtil.clearDataFromTable;
import static org.folio.util.TestUtil.readFile;
import static org.folio.util.TestUtil.readJsonFile;
import static org.folio.util.UcCredentialsTestUtil.setUpUcCredentials;
import static org.folio.util.UcSettingsTestUtil.saveUcSettings;
import static org.folio.util.UcSettingsTestUtil.stubSettings;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.OffsetDateTime;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.PackageCostPerUse;
import org.folio.rest.jaxrs.model.ResourceCostPerUse;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollection;
import org.folio.rest.jaxrs.model.TitleCostPerUse;
import org.folio.util.IntegrationTestBase;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EholdingsCostperuseImplIntegrationTest extends IntegrationTestBase {

  // RM API responses
  private static final String GET_TITLE_WITH_COVERAGE_DATES_ASC =
    "responses/rmapi/titles/get-custom-title-with-coverage-dates-asc.json";
  private static final String GET_TITLE_WITH_MANAGED_EMBARGO =
    "responses/rmapi/titles/get-custom-title-with-managed-embargo.json";
  private static final String GET_TITLE_WITH_NO_SELECTED_RESOURCES =
    "responses/rmapi/titles/get-custom-title-with-no-selected-resources.json";

  // UC responses
  private static final String UC_TITLE_COST_PER_USE =
    "responses/uc/titles/get-title-cost-per-use-response.json";
  private static final String UC_EMPTY_TITLE_COST_PER_USE =
    "responses/uc/titles/get-empty-title-cost-per-use-response.json";
  private static final String UC_TITLE_COST_PER_USE_WITH_EMPTY_NON_PUBLISHER =
    "responses/uc/titles/get-title-cost-per-use-response-with-empty-non-publisher.json";
  private static final String UC_PACKAGE_COST_PER_USE =
    "responses/uc/packages/get-package-cost-per-use-response.json";
  private static final String UC_PACKAGE_COST_PER_USE_EMPTY_COST =
    "responses/uc/packages/get-package-cost-per-use-with-empty-cost-response.json";
  private static final String UC_TITLE_PACKAGES_COST_PER_USE =
    "responses/uc/title-packages/get-title-packages-cost-per-use-response.json";
  private static final String UC_TITLE_PACKAGES_COST_PER_USE_FOR_PACKAGE =
    "responses/uc/title-packages/get-title-packages-cost-per-use-for-package-response.json";
  private static final String UC_MULTIPLY_TITLE_PACKAGES_COST_PER_USE_FOR_PACKAGE =
    "responses/uc/title-packages/get-multiply-title-packages-cost-per-use-for-package-response.json";
  private static final String UC_DIFFERENT_TITLE_PACKAGES_COST_PER_USE_FOR_PACKAGE =
    "responses/uc/title-packages/get-different-title-packages-cost-per-use-for-package-response.json";

  // KB-EBSCO expected responses
  private static final String EXPECTED_RESOURCE_COST_PER_USE =
    "responses/kb-ebsco/costperuse/resources/expected-resource-cost-per-use.json";
  private static final String EXPECTED_EMPTY_RESOURCE_COST_PER_USE =
    "responses/kb-ebsco/costperuse/resources/expected-empty-resource-cost-per-use.json";
  private static final String EXPECTED_RESOURCE_COST_PER_USE_WITH_EMPTY_NON_PUBLISHER =
    "responses/kb-ebsco/costperuse/resources/expected-resource-cost-per-use-with-empty-non-publisher.json";
  private static final String EXPECTED_TITLE_COST_PER_USE =
    "responses/kb-ebsco/costperuse/titles/expected-title-cost-per-use.json";
  private static final String EXPECTED_TITLE_COST_PER_USE_WITH_NO_USAGE =
    "responses/kb-ebsco/costperuse/titles/expected-title-cost-per-use-with-no-usage.json";
  private static final String EXPECTED_EMPTY_TITLE_COST_PER_USE =
    "responses/kb-ebsco/costperuse/titles/expected-empty-title-cost-per-use.json";
  private static final String EXPECTED_PACKAGE_COST_PER_USE =
    "responses/kb-ebsco/costperuse/packages/expected-package-cost-per-use.json";
  private static final String EXPECTED_PACKAGE_COST_PER_USE_WHEN_COST_IS_EMPTY =
    "responses/kb-ebsco/costperuse/packages/expected-package-cost-per-use-when-cost-is-empty.json";

  private final EasyRandom random = new EasyRandom();

  private String credentialsId;

  @BeforeEach
  void setUp() {
    credentialsId = setupDefaultKbConfiguration(getWiremockUrl(), vertx);
    saveUcSettings(stubSettings(credentialsId), vertx);
    setUpUcCredentials(vertx);
    mockAuthToken();
  }

  @AfterEach
  void after() {
    clearDataFromTable(vertx, UC_CREDENTIALS_TABLE_NAME);
    clearDataFromTable(vertx, UC_SETTINGS_TABLE_NAME);
    clearDataFromTable(vertx, HOLDINGS_TABLE);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  void shouldReturnResourceCostPerUse() {
    int titleId = 356;
    int packageId = 473;
    String year = "2019";
    String platform = "all";
    mockSuccessfulTitleCostPerUse(titleId, packageId, UC_TITLE_COST_PER_USE);

    var expected = readJsonFile(EXPECTED_RESOURCE_COST_PER_USE, ResourceCostPerUse.class);
    var actual = getWithOk(resourceEndpoint(titleId, packageId, year, platform))
      .as(ResourceCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  void shouldReturnEmptyResourceCostPerUse() {
    int titleId = 356;
    int packageId = 473;
    String year = "2019";
    mockSuccessfulTitleCostPerUse(titleId, packageId, UC_EMPTY_TITLE_COST_PER_USE);

    var expected = readJsonFile(EXPECTED_EMPTY_RESOURCE_COST_PER_USE, ResourceCostPerUse.class);
    var actual = getWithOk(resourceEndpoint(titleId, packageId, year, null))
      .as(ResourceCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  void shouldReturnResourceCostPerUseWithEmptyNonPublisher() {
    int titleId = 356;
    int packageId = 473;
    String year = "2019";
    String platform = "all";
    mockSuccessfulTitleCostPerUse(titleId, packageId, UC_TITLE_COST_PER_USE_WITH_EMPTY_NON_PUBLISHER);

    var expected = readJsonFile(EXPECTED_RESOURCE_COST_PER_USE_WITH_EMPTY_NON_PUBLISHER, ResourceCostPerUse.class);
    var actual = getWithOk(resourceEndpoint(titleId, packageId, year, platform)).as(ResourceCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  void shouldReturn422OnGetResourceCpuWhenYearIsNull() {
    int titleId = 356;
    int packageId = 473;
    var error = getWithStatus(resourceEndpoint(titleId, packageId, null, null), SC_UNPROCESSABLE_CONTENT)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid fiscalYear");
  }

  @Test
  void shouldReturn422OnGetResourceCpuWhenPlatformIsInvalid() {
    int titleId = 356;
    int packageId = 473;
    String year = "2019";
    String platform = "invalid";
    var error = getWithStatus(resourceEndpoint(titleId, packageId, year, platform), SC_UNPROCESSABLE_CONTENT)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid platform");
  }

  @Test
  void shouldReturn400OnGetResourceCpuWhenApigeeFails() {
    int titleId = 356;
    int packageId = 473;
    String year = "2019";
    mockGet(matching("/uc/costperuse/title/%s/%s".formatted(titleId, packageId)), "Random error message",
      SC_BAD_REQUEST);

    var error = getWithStatus(resourceEndpoint(titleId, packageId, year, null), SC_BAD_REQUEST).as(JsonapiError.class);

    assertErrorContainsDetail(error, "Random error message");
  }

  @Test
  void shouldReturnTitleCostPerUse() {
    int titleId = 1111111111;
    int packageId = 222222;
    final String year = "2019";
    final String platform = "all";

    mockRmApiGetTitle(titleId, GET_TITLE_WITH_COVERAGE_DATES_ASC);
    mockSuccessfulTitleCostPerUse(titleId, packageId, UC_TITLE_COST_PER_USE);
    mockSuccessfulTitlePackageCostPerUse(UC_TITLE_PACKAGES_COST_PER_USE);

    var expected = readJsonFile(EXPECTED_TITLE_COST_PER_USE, TitleCostPerUse.class);
    var actual = getWithOk(titleEndpoint(titleId, year, platform)).as(TitleCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  void shouldReturnTitleCostPerUseWithManagedEmbargoPeriod() {
    int titleId = 1111111111;
    int packageId = 222222;
    final String year = "2019";
    final String platform = "nonPublisher";

    mockRmApiGetTitle(titleId, GET_TITLE_WITH_MANAGED_EMBARGO);
    mockSuccessfulTitleCostPerUse(titleId, packageId, UC_EMPTY_TITLE_COST_PER_USE);
    mockSuccessfulTitlePackageCostPerUse(UC_TITLE_PACKAGES_COST_PER_USE);

    var expected = readJsonFile(EXPECTED_TITLE_COST_PER_USE_WITH_NO_USAGE, TitleCostPerUse.class);
    var actual = getWithOk(titleEndpoint(titleId, year, platform)).as(TitleCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  void shouldReturnEmptyTitleCostPerUseWhenNoCostPerUseDataAvailable() {
    int titleId = 1111111111;
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";

    mockRmApiGetTitle(titleId, GET_TITLE_WITH_COVERAGE_DATES_ASC);
    mockSuccessfulTitleCostPerUse(titleId, packageId, UC_EMPTY_TITLE_COST_PER_USE);
    mockSuccessfulTitlePackageCostPerUse(UC_TITLE_PACKAGES_COST_PER_USE);

    var expected = readJsonFile(EXPECTED_TITLE_COST_PER_USE_WITH_NO_USAGE, TitleCostPerUse.class);
    var actual = getWithOk(titleEndpoint(titleId, year, platform)).as(TitleCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  void shouldReturnEmptyTitleCostPerUseWhenNoSelectedResources() {
    int titleId = 1111111111;
    String year = "2019";
    String platform = "all";

    mockRmApiGetTitle(titleId, GET_TITLE_WITH_NO_SELECTED_RESOURCES);

    var expected = readJsonFile(EXPECTED_EMPTY_TITLE_COST_PER_USE, TitleCostPerUse.class);
    var actual = getWithOk(titleEndpoint(titleId, year, platform)).as(TitleCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  void shouldReturn422OnGetTitleCpuWhenYearIsNull() {
    int titleId = 356;
    var error = getWithStatus(titleEndpoint(titleId, null, null), SC_UNPROCESSABLE_CONTENT)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid fiscalYear");
  }

  @Test
  void shouldReturn422OnGetTitleCpuWhenPlatformIsInvalid() {
    int titleId = 356;
    String year = "2019";
    String platform = "invalid";
    var error = getWithStatus(titleEndpoint(titleId, year, platform), SC_UNPROCESSABLE_CONTENT)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid platform");
  }

  @Test
  void shouldReturnPackageCostPerUse() {
    int packageId = 222222;
    String year = "2019";
    String platform = "all";

    mockSuccessfulPackageCostPerUse(packageId, UC_PACKAGE_COST_PER_USE);

    var expected = readJsonFile(EXPECTED_PACKAGE_COST_PER_USE, PackageCostPerUse.class);
    var actual = getWithOk(packageEndpoint(packageId, year, platform)).as(PackageCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  void shouldReturnPackageCostPerUseWhenPackageCostIsEmpty() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "all";

    var holding1 = new DbHoldingInfo(1, packageId, 1, "Ionicis tormentos accelerare!", "Sunt hydraes", "Book");
    var holding2 = new DbHoldingInfo(2, packageId, 1, "Vortex, plasmator, et lixa.", "Est germanus byssus", "Book");
    saveHolding(credentialsId, holding1, OffsetDateTime.now(), vertx);
    saveHolding(credentialsId, holding2, OffsetDateTime.now(), vertx);

    mockSuccessfulPackageCostPerUse(packageId, UC_PACKAGE_COST_PER_USE_EMPTY_COST);
    mockSuccessfulTitlePackageCostPerUse(UC_TITLE_PACKAGES_COST_PER_USE_FOR_PACKAGE);

    var expected = readJsonFile(EXPECTED_PACKAGE_COST_PER_USE_WHEN_COST_IS_EMPTY, PackageCostPerUse.class);
    var actual = getWithOk(packageEndpoint(packageId, year, platform)).as(PackageCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  void shouldReturn422OnGetPackageCpuWhenYearIsNull() {
    int packageId = 222222;
    var error = getWithStatus(packageEndpoint(packageId, null, null), SC_UNPROCESSABLE_CONTENT)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid fiscalYear");
  }

  @Test
  void shouldReturn422OnGetPackageCpuWhenPlatformIsInvalid() {
    int packageId = 222222;
    String year = "2019";
    String platform = "invalid";
    var error = getWithStatus(packageEndpoint(packageId, year, platform), SC_UNPROCESSABLE_CONTENT)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid platform");
  }

  @Test
  void shouldReturn400OnGetPackageCpuWhenApigeeFails() {
    int packageId = 222222;
    String year = "2019";
    mockGet(matching("/uc/costperuse/package/%s".formatted(packageId)), "Random error message", SC_BAD_REQUEST);

    var error = getWithStatus(packageEndpoint(packageId, year, null), SC_BAD_REQUEST)
      .as(JsonapiError.class);

    assertErrorContainsDetail(error, "Random error message");
  }

  @Test
  void shouldReturnResourcesCostPerUseCollection() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "all";

    for (int i = 1; i <= 20; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    mockSuccessfulPackageCostPerUse(packageId, UC_PACKAGE_COST_PER_USE_EMPTY_COST);
    mockSuccessfulTitlePackageCostPerUse(UC_MULTIPLY_TITLE_PACKAGES_COST_PER_USE_FOR_PACKAGE);

    var actual = getWithOk(packageResourcesEndpoint(packageId, year, platform, null, null))
      .as(ResourceCostPerUseCollection.class);

    assertNotNull(actual);
    assertEquals(20, actual.getMeta().getTotalResults());
    assertEquals(20, actual.getData().size());
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().getFirst().getAttributes(), hasProperty("usage", equalTo(2)));
    assertThat(actual.getData().getFirst().getAttributes(), hasProperty("percent", equalTo(2.0 / 36 * 100)));
  }

  @Test
  void shouldReturnResourcesCostPerUseCollectionWithPagination() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";
    final var page = "2";
    final var size = "15";

    for (int i = 1; i <= 20; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    mockSuccessfulPackageCostPerUse(packageId, UC_PACKAGE_COST_PER_USE_EMPTY_COST);
    mockSuccessfulTitlePackageCostPerUse(UC_MULTIPLY_TITLE_PACKAGES_COST_PER_USE_FOR_PACKAGE);

    var actual =
      getWithOk(packageResourcesEndpoint(packageId, year, platform, page, size))
        .as(ResourceCostPerUseCollection.class);

    assertNotNull(actual);
    assertEquals(20, actual.getMeta().getTotalResults());
    assertEquals(5, actual.getData().size());
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().getFirst().getAttributes(), hasProperty("usage", equalTo(1)));
  }

  @Test
  void shouldReturnResourcesCostPerUseCollectionWithSortByUsageAsc() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";
    final String sort = "usage";
    final String order = "asc";

    for (int i = 1; i <= 3; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    mockSuccessfulPackageCostPerUse(packageId, UC_PACKAGE_COST_PER_USE_EMPTY_COST);
    mockSuccessfulTitlePackageCostPerUse(UC_DIFFERENT_TITLE_PACKAGES_COST_PER_USE_FOR_PACKAGE);

    var actual =
      getWithOk(packageResourcesEndpoint(packageId, year, platform, null, null, sort, order))
        .as(ResourceCostPerUseCollection.class);

    assertNotNull(actual);
    assertEquals(3, actual.getMeta().getTotalResults());
    assertEquals(3, actual.getData().size());
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().get(0).getAttributes(), hasProperty("usage", equalTo(1)));
    assertThat(actual.getData().get(2).getAttributes(), hasProperty("usage", equalTo(10)));
  }

  @Test
  void shouldReturnResourcesCostPerUseCollectionWithSortByUsageDesc() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";
    final String sort = "usage";
    final String order = "desc";

    for (int i = 1; i <= 3; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    mockSuccessfulPackageCostPerUse(packageId, UC_PACKAGE_COST_PER_USE_EMPTY_COST);
    mockSuccessfulTitlePackageCostPerUse(UC_DIFFERENT_TITLE_PACKAGES_COST_PER_USE_FOR_PACKAGE);

    var actual =
      getWithOk(packageResourcesEndpoint(packageId, year, platform, null, null, sort, order))
        .as(ResourceCostPerUseCollection.class);

    assertNotNull(actual);
    assertEquals(3, actual.getMeta().getTotalResults());
    assertEquals(3, actual.getData().size());
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().get(0).getAttributes(), hasProperty("usage", equalTo(10)));
    assertThat(actual.getData().get(2).getAttributes(), hasProperty("usage", equalTo(1)));
  }

  @Test
  void shouldReturnResourcesCostPerUseCollectionWithSortByCost() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";
    final String sort = "cost";

    for (int i = 1; i <= 3; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    mockSuccessfulPackageCostPerUse(packageId, UC_PACKAGE_COST_PER_USE_EMPTY_COST);
    mockSuccessfulTitlePackageCostPerUse(UC_DIFFERENT_TITLE_PACKAGES_COST_PER_USE_FOR_PACKAGE);

    var actual =
      getWithOk(packageResourcesEndpoint(packageId, year, platform, null, null, sort, null))
        .as(ResourceCostPerUseCollection.class);

    assertNotNull(actual);
    assertEquals(3, actual.getMeta().getTotalResults());
    assertEquals(3, actual.getData().size());
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().get(0).getAttributes(), hasProperty("cost", nullValue()));
    assertThat(actual.getData().get(2).getAttributes(), hasProperty("cost", equalTo(200.0)));
  }

  @Test
  void shouldReturnResourcesCostPerUseCollectionWithSortByCostPerUse() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";
    final String sort = "costperuse";

    for (int i = 1; i <= 3; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    mockSuccessfulPackageCostPerUse(packageId, UC_PACKAGE_COST_PER_USE_EMPTY_COST);
    mockSuccessfulTitlePackageCostPerUse(UC_DIFFERENT_TITLE_PACKAGES_COST_PER_USE_FOR_PACKAGE);

    var actual =
      getWithOk(packageResourcesEndpoint(packageId, year, platform, null, null, sort, null))
        .as(ResourceCostPerUseCollection.class);

    assertNotNull(actual);
    assertEquals(3, actual.getMeta().getTotalResults());
    assertEquals(3, actual.getData().size());
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().get(0).getAttributes(), hasProperty("costPerUse", nullValue()));
    assertThat(actual.getData().get(2).getAttributes(), hasProperty("costPerUse", equalTo(50.0)));
  }

  @Test
  void shouldReturnResourcesCostPerUseCollectionWithSortByPercent() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";
    final String sort = "percent";

    for (int i = 1; i <= 3; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    mockSuccessfulPackageCostPerUse(packageId, UC_PACKAGE_COST_PER_USE_EMPTY_COST);
    mockSuccessfulTitlePackageCostPerUse(UC_DIFFERENT_TITLE_PACKAGES_COST_PER_USE_FOR_PACKAGE);

    var actual =
      getWithOk(packageResourcesEndpoint(packageId, year, platform, null, null, sort, null))
        .as(ResourceCostPerUseCollection.class);

    assertNotNull(actual);
    assertEquals(3, actual.getMeta().getTotalResults());
    assertEquals(3, actual.getData().size());
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().get(0).getAttributes(), hasProperty("percent", equalTo(1.0 / 26 * 100)));
    assertThat(actual.getData().get(2).getAttributes(), hasProperty("percent", equalTo(10.0 / 26 * 100)));
  }

  @Test
  void shouldReturnResourcesCostPerUseCollectionSortedByNameWhenSortingByEqualsValues() {
    int packageId = 222222;
    final String year = "2019";
    final String platform = "publisher";
    final String sort = "type";

    for (int i = 1; i <= 3; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i, String.valueOf(i)), OffsetDateTime.now(), vertx);
    }

    mockSuccessfulPackageCostPerUse(packageId, UC_PACKAGE_COST_PER_USE_EMPTY_COST);
    mockSuccessfulTitlePackageCostPerUse(UC_DIFFERENT_TITLE_PACKAGES_COST_PER_USE_FOR_PACKAGE);

    var actual =
      getWithOk(packageResourcesEndpoint(packageId, year, platform, null, null, sort, null))
        .as(ResourceCostPerUseCollection.class);

    assertNotNull(actual);
    assertEquals(3, actual.getMeta().getTotalResults());
    assertEquals(3, actual.getData().size());
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().get(0).getAttributes(), hasProperty("name", equalTo("1")));
    assertThat(actual.getData().get(2).getAttributes(), hasProperty("name", equalTo("3")));
  }

  @Test
  void shouldReturn422OnGetPackageResourcesCpuWhenYearIsNull() {
    int packageId = 222222;
    var error =
      getWithStatus(packageResourcesEndpoint(packageId, null, null, null, null), SC_UNPROCESSABLE_CONTENT)
        .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid fiscalYear");
  }

  @Test
  void shouldReturn422OnGetPackageResourcesCpuWhenPlatformIsInvalid() {
    int packageId = 222222;
    String year = "2019";
    String platform = "invalid";
    var error =
      getWithStatus(packageResourcesEndpoint(packageId, year, platform, null, null), SC_UNPROCESSABLE_CONTENT)
        .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid platform");
  }

  @Test
  void shouldReturn422OnGetPackageResourcesCpuWhenSortIsInvalid() {
    int packageId = 222222;
    String year = "2019";
    String platform = "all";
    String sort = "invalid";
    var error =
      getWithStatus(packageResourcesEndpoint(packageId, year, platform, null, null, sort, null),
        SC_UNPROCESSABLE_CONTENT)
        .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid sort");
  }

  @Test
  void shouldReturn400OnGetPackageResourcesCpuWhenApigeeFails() {
    final int packageId = 222222;
    final String year = "2019";

    saveHolding(credentialsId, generateHolding(packageId, 1), OffsetDateTime.now(), vertx);

    mockSuccessfulPackageCostPerUse(packageId, UC_PACKAGE_COST_PER_USE_EMPTY_COST);
    mockPost(matching("/uc/costperuse/titles"), "Random error message", SC_BAD_REQUEST);

    var error =
      getWithStatus(packageResourcesEndpoint(packageId, year, null, null, null), SC_BAD_REQUEST)
        .as(JsonapiError.class);

    assertErrorContainsDetail(error, "Random error message");
  }

  private void mockRmApiGetTitle(int titleId, String stubRmapiResponseFile) {
    mockGet(matching(titlesRmApi(titleId)), readFile(stubRmapiResponseFile));
  }

  private void mockSuccessfulTitleCostPerUse(int titleId, int packageId, String filePath) {
    mockGet(matching("/uc/costperuse/title/%s/%s".formatted(titleId, packageId)), readFile(filePath));
  }

  private void mockSuccessfulPackageCostPerUse(int packageId, String filePath) {
    mockGet(matching("/uc/costperuse/package/%s".formatted(packageId)), readFile(filePath));
  }

  private void mockSuccessfulTitlePackageCostPerUse(String filePath) {
    mockPost(matching("/uc/costperuse/titles"), readFile(filePath), SC_OK);
  }

  private String resourceEndpoint(int titleId, int packageId, String year, String platform) {
    var baseUrl = "eholdings/resources/1-%s-%s/costperuse".formatted(packageId, titleId);
    var paramsSb = getEndpointParams(year, platform);
    return !paramsSb.isEmpty() ? baseUrl + "?" + paramsSb : baseUrl;
  }

  private String titleEndpoint(int titleId, String year, String platform) {
    var baseUrl = "eholdings/titles/%s/costperuse".formatted(titleId);
    var paramsSb = getEndpointParams(year, platform);
    return !paramsSb.isEmpty() ? baseUrl + "?" + paramsSb : baseUrl;
  }

  private String packageEndpoint(int packageId, String year, String platform) {
    var baseUrl = "eholdings/packages/1-%s/costperuse".formatted(packageId);
    var paramsSb = getEndpointParams(year, platform);
    return !paramsSb.isEmpty() ? baseUrl + "?" + paramsSb : baseUrl;
  }

  private String packageResourcesEndpoint(int packageId, String year, String platform, String page, String size) {
    return packageResourcesEndpoint(packageId, year, platform, page, size, null, null);
  }

  private String packageResourcesEndpoint(int packageId, String year, String platform, String page, String size,
                                          String sort, String order) {
    var baseUrl = "eholdings/packages/1-%s/resources/costperuse".formatted(packageId);
    var paramsSb = getEndpointParams(year, platform, page, size, sort, order);
    return !paramsSb.isEmpty() ? baseUrl + "?" + paramsSb : baseUrl;
  }

  private StringBuilder getEndpointParams(String year, String platform) {
    return getEndpointParams(year, platform, null, null, null, null);
  }

  private StringBuilder getEndpointParams(String year, String platform, String page, String size, String sort,
                                          String order) {
    var paramsSb = new StringBuilder();
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

  private void addParam(String value, StringBuilder paramsSb, String key) {
    if (value != null) {
      if (!paramsSb.isEmpty()) {
        paramsSb.append("&");
      }
      paramsSb.append(key).append(value);
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
