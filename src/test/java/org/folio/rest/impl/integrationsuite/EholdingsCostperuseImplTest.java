package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;

import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UCCredentialsTableConstants.UC_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UCSettingsTableConstants.UC_SETTINGS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsDetail;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.HoldingsTestUtil.saveHolding;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.UCCredentialsTestUtil.setUpUCCredentials;
import static org.folio.util.UCSettingsTestUtil.saveUCSettings;
import static org.folio.util.UCSettingsTestUtil.stubSettings;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;

import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.jeasy.random.EasyRandom;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import org.folio.client.uc.UCApigeeEbscoClient;
import org.folio.client.uc.UCAuthEbscoClient;
import org.folio.client.uc.model.UCAuthToken;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.PackageCostPerUse;
import org.folio.rest.jaxrs.model.ResourceCostPerUse;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollection;
import org.folio.rest.jaxrs.model.TitleCostPerUse;
import org.folio.test.util.TestUtil;

@RunWith(VertxUnitRunner.class)
public class EholdingsCostperuseImplTest extends WireMockTestBase {

  private final EasyRandom random = new EasyRandom();

  @Autowired
  private UCAuthEbscoClient authEbscoClient;
  @Autowired
  private UCApigeeEbscoClient apigeeEbscoClient;

  private String credentialsId;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    credentialsId = saveKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
    String credentialsId = this.credentialsId;
    saveUCSettings(stubSettings(credentialsId), vertx);
    setUpUCCredentials(vertx);

    ReflectionTestUtils.setField(authEbscoClient, "baseUrl", getWiremockUrl());
    ReflectionTestUtils.setField(apigeeEbscoClient, "baseUrl", getWiremockUrl());
    mockAuthToken();
  }

  @After
  public void after() {
    clearDataFromTable(vertx, UC_CREDENTIALS_TABLE_NAME);
    clearDataFromTable(vertx, UC_SETTINGS_TABLE_NAME);
    clearDataFromTable(vertx, KB_CREDENTIALS_TABLE_NAME);
  }

  @Test
  public void shouldReturnResourceCostPerUse() {
    String titleId = "356";
    String packageId = "473";
    String year = "2019";
    String platform = "all";
    String stubApigeeResponseFile = "responses/uc/titles/get-title-cost-per-use-response.json";
    mockSuccessfulTitleCostPerUse(titleId, packageId, stubApigeeResponseFile);

    String kbEbscoResponseFile = "responses/kb-ebsco/costperuse/resources/expected-resource-cost-per-use.json";
    ResourceCostPerUse expected = Json.decodeValue(readFile(kbEbscoResponseFile), ResourceCostPerUse.class);

    ResourceCostPerUse actual = getWithOk(resourceEndpoint(titleId, packageId, year, platform), JOHN_TOKEN_HEADER)
      .as(ResourceCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturnEmptyResourceCostPerUse() {
    String titleId = "356";
    String packageId = "473";
    String year = "2019";
    String stubApigeeResponseFile = "responses/uc/titles/get-empty-title-cost-per-use-response.json";
    mockSuccessfulTitleCostPerUse(titleId, packageId, stubApigeeResponseFile);

    String kbEbscoResponseFile = "responses/kb-ebsco/costperuse/resources/expected-empty-resource-cost-per-use.json";
    ResourceCostPerUse expected = Json.decodeValue(readFile(kbEbscoResponseFile), ResourceCostPerUse.class);

    ResourceCostPerUse actual = getWithOk(resourceEndpoint(titleId, packageId, year, null), JOHN_TOKEN_HEADER)
      .as(ResourceCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturn422OnGetResourceCPUWhenYearIsNull() {
    String titleId = "356";
    String packageId = "473";
    JsonapiError error = getWithStatus(resourceEndpoint(titleId, packageId, null, null), SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid fiscalYear");
  }

  @Test
  public void shouldReturn422OnGetResourceCPUWhenPlatformIsInvalid() {
    String titleId = "356";
    String packageId = "473";
    String year = "2019";
    String platform = "invalid";
    JsonapiError error = getWithStatus(resourceEndpoint(titleId, packageId, year, platform), SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid platform");
  }

  @Test
  public void shouldReturn400OnGetResourceCPUWhenApigeeFails() {
    String titleId = "356";
    String packageId = "473";
    String year = "2019";

    stubFor(get(urlPathMatching(String.format("/uc/costperuse/title/%s/%s", titleId, packageId)))
      .willReturn(aResponse().withStatus(SC_BAD_REQUEST).withBody("Random error message"))
    );

    JsonapiError error = getWithStatus(resourceEndpoint(titleId, packageId, year, null), SC_BAD_REQUEST, JOHN_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsDetail(error, "Random error message");
  }

  @Test
  public void shouldReturnTitleCostPerUse() {
    String titleId = "1111111111";
    String packageId = "222222";
    String year = "2019";
    String platform = "all";
    String stubRmapiResponseFile = "responses/rmapi/titles/get-custom-title-with-coverage-dates-asc.json";

    mockRmApiGetTitle(titleId, stubRmapiResponseFile);
    String stubApigeeGetTitleResponseFile = "responses/uc/titles/get-title-cost-per-use-response.json";
    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-title-packages-cost-per-use-response.json";
    mockSuccessfulTitleCostPerUse(titleId, packageId, stubApigeeGetTitleResponseFile);
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    String kbEbscoResponseFile = "responses/kb-ebsco/costperuse/titles/expected-title-cost-per-use.json";
    TitleCostPerUse expected = Json.decodeValue(readFile(kbEbscoResponseFile), TitleCostPerUse.class);

    TitleCostPerUse actual = getWithOk(titleEndpoint(titleId, year, platform), JOHN_TOKEN_HEADER)
      .as(TitleCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturnEmptyTitleCostPerUseWhenNoSelectedResources() {
    String titleId = "1111111111";
    String year = "2019";
    String platform = "all";

    String stubRmapiResponseFile = "responses/rmapi/titles/get-custom-title-with-no-selected-resources.json";
    mockRmApiGetTitle(titleId, stubRmapiResponseFile);

    String kbEbscoResponseFile = "responses/kb-ebsco/costperuse/titles/expected-empty-title-cost-per-use.json";
    TitleCostPerUse expected = Json.decodeValue(readFile(kbEbscoResponseFile), TitleCostPerUse.class);

    TitleCostPerUse actual = getWithOk(titleEndpoint(titleId, year, platform), JOHN_TOKEN_HEADER)
      .as(TitleCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturn422OnGetTitleCPUWhenYearIsNull() {
    String titleId = "356";
    JsonapiError error = getWithStatus(titleEndpoint(titleId, null, null), SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid fiscalYear");
  }

  @Test
  public void shouldReturn422OnGetTitleCPUWhenPlatformIsInvalid() {
    String titleId = "356";
    String year = "2019";
    String platform = "invalid";
    JsonapiError error = getWithStatus(titleEndpoint(titleId, year, platform), SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid platform");
  }

  @Test
  public void shouldReturn400OnGetTitleCPUWhenApigeeFails() {
    String titleId = "356";
    String packageId = "473";
    String year = "2019";

    stubFor(get(urlPathMatching(String.format("/uc/costperuse/title/%s/%s", titleId, packageId)))
      .willReturn(aResponse().withStatus(SC_BAD_REQUEST).withBody("Random error message"))
    );

    JsonapiError error = getWithStatus(resourceEndpoint(titleId, packageId, year, null), SC_BAD_REQUEST, JOHN_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsDetail(error, "Random error message");
  }

  @Test
  public void shouldReturnPackageCostPerUse() {
    String packageId = "222222";
    String year = "2019";
    String platform = "all";

    String stubApigeeGetPackageResponseFile = "responses/uc/packages/get-package-cost-per-use-response.json";
    mockSuccessfulPackageCostPerUse(packageId, stubApigeeGetPackageResponseFile);

    String kbEbscoResponseFile = "responses/kb-ebsco/costperuse/packages/expected-package-cost-per-use.json";
    PackageCostPerUse expected = Json.decodeValue(readFile(kbEbscoResponseFile), PackageCostPerUse.class);

    PackageCostPerUse actual = getWithOk(packageEndpoint(packageId, year, platform), JOHN_TOKEN_HEADER)
      .as(PackageCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturnPackageCostPerUseWhenPackageCostIsEmpty() {
    String packageId = "222222";
    String year = "2019";
    String platform = "all";

    var holding1 = new DbHoldingInfo("1", packageId, "1", "Ionicis tormentos accelerare!", "Sunt hydraes", "Book");
    var holding2 = new DbHoldingInfo("2", packageId, "1", "Vortex, plasmator, et lixa.", "Est germanus byssus", "Book");
    saveHolding(credentialsId, holding1, OffsetDateTime.now(), vertx);
    saveHolding(credentialsId, holding2, OffsetDateTime.now(), vertx);

    String stubApigeeGetPackageResponseFile = "responses/uc/packages/get-package-cost-per-use-with-empty-cost-response.json";
    mockSuccessfulPackageCostPerUse(packageId, stubApigeeGetPackageResponseFile);
    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-title-packages-cost-per-use-for-package-response.json";
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    String kbEbscoResponseFile =
      "responses/kb-ebsco/costperuse/packages/expected-package-cost-per-use-when-cost-is-empty.json";
    PackageCostPerUse expected = Json.decodeValue(readFile(kbEbscoResponseFile), PackageCostPerUse.class);

    PackageCostPerUse actual = getWithOk(packageEndpoint(packageId, year, platform), JOHN_TOKEN_HEADER)
      .as(PackageCostPerUse.class);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturn422OnGetPackageCPUWhenYearIsNull() {
    String packageId = "222222";
    JsonapiError error = getWithStatus(packageEndpoint(packageId, null, null), SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid fiscalYear");
  }

  @Test
  public void shouldReturn422OnGetPackageCPUWhenPlatformIsInvalid() {
    String packageId = "222222";
    String year = "2019";
    String platform = "invalid";
    JsonapiError error = getWithStatus(packageEndpoint(packageId, year, platform), SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid platform");
  }

  @Test
  public void shouldReturn400OnGetPackageCPUWhenApigeeFails() {
    String packageId = "222222";
    String year = "2019";

    stubFor(get(urlPathMatching(String.format("/uc/costperuse/package/%s", packageId)))
      .willReturn(aResponse().withStatus(SC_BAD_REQUEST).withBody("Random error message"))
    );

    JsonapiError error = getWithStatus(packageEndpoint(packageId, year, null), SC_BAD_REQUEST, JOHN_TOKEN_HEADER)
      .as(JsonapiError.class);

    assertErrorContainsDetail(error, "Random error message");
  }

  @Test
  public void shouldReturnResourcesCostPerUseCollection() {
    String packageId = "222222";
    String year = "2019";
    String platform = "all";

    for (int i = 1; i <= 20; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-multiply-title-packages-cost-per-use-for-package-response.json";
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    ResourceCostPerUseCollection
      actual = getWithOk(packageResourcesEndpoint(packageId, year, platform, null, null), JOHN_TOKEN_HEADER)
      .as(ResourceCostPerUseCollection.class);

    assertThat(actual, notNullValue());
    assertThat(actual.getMeta().getTotalResults(), equalTo(20));
    assertThat(actual.getData(), hasSize(20));
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().get(0).getAttributes(), hasProperty("usage", equalTo(2)));
  }

  @Test
  public void shouldReturnResourcesCostPerUseCollectionWithPagination() {
    String packageId = "222222";
    String year = "2019";
    String platform = "publisher";
    var page = "2";
    var size = "15";

    for (int i = 1; i <= 20; i++) {
      saveHolding(credentialsId, generateHolding(packageId, i), OffsetDateTime.now(), vertx);
    }

    String stubApigeeGetTitlePackageResponseFile =
      "responses/uc/title-packages/get-multiply-title-packages-cost-per-use-for-package-response.json";
    mockSuccessfulTitlePackageCostPerUse(stubApigeeGetTitlePackageResponseFile);

    ResourceCostPerUseCollection actual =
      getWithOk(packageResourcesEndpoint(packageId, year, platform, page, size), JOHN_TOKEN_HEADER)
        .as(ResourceCostPerUseCollection.class);

    assertThat(actual, notNullValue());
    assertThat(actual.getMeta().getTotalResults(), equalTo(20));
    assertThat(actual.getData(), hasSize(5));
    assertThat(actual.getData(), everyItem(hasProperty("resourceId", startsWith("1-" + packageId))));
    assertThat(actual.getData().get(0).getAttributes(), hasProperty("usage", equalTo(1)));
  }

  @Test
  public void shouldReturn422OnGetPackageResourcesCPUWhenYearIsNull() {
    String packageId = "222222";
    JsonapiError error = getWithStatus(packageResourcesEndpoint(packageId, null, null, null, null), SC_UNPROCESSABLE_ENTITY)
      .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid fiscalYear");
  }

  @Test
  public void shouldReturn422OnGetPackageResourcesCPUWhenPlatformIsInvalid() {
    String packageId = "222222";
    String year = "2019";
    String platform = "invalid";
    JsonapiError error =
      getWithStatus(packageResourcesEndpoint(packageId, year, platform, null, null), SC_UNPROCESSABLE_ENTITY)
        .as(JsonapiError.class);

    assertErrorContainsTitle(error, "Invalid platform");
  }

  @Test
  public void shouldReturn400OnGetPackageResourcesCPUWhenApigeeFails() {
    String packageId = "222222";
    String year = "2019";

    stubFor(get(urlPathMatching(String.format("/uc/costperuse/package/%s", packageId)))
      .willReturn(aResponse().withStatus(SC_BAD_REQUEST).withBody("Random error message"))
    );

    JsonapiError error =
      getWithStatus(packageResourcesEndpoint(packageId, year, null, null, null), SC_BAD_REQUEST, JOHN_TOKEN_HEADER)
        .as(JsonapiError.class);

    assertErrorContainsDetail(error, "Random error message");
  }

  private void mockRmApiGetTitle(String titleId, String stubRmapiResponseFile) {
    stubFor(get(urlPathMatching("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles/" + titleId))
      .willReturn(aResponse().withBody(readFile(stubRmapiResponseFile)))
    );
  }

  private void mockSuccessfulTitleCostPerUse(String titleId, String packageId, String filePath) {
    stubFor(get(urlPathMatching(String.format("/uc/costperuse/title/%s/%s", titleId, packageId)))
      .willReturn(aResponse().withStatus(SC_OK).withBody(readFile(filePath)))
    );
  }

  private void mockSuccessfulPackageCostPerUse(String packageId, String filePath) {
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
    UCAuthToken stubToken = new UCAuthToken("access_token", "Bearer", 3600L, "openid");
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

  private String resourceEndpoint(String titleId, String packageId, String year, String platform) {
    String baseUrl = String.format("eholdings/resources/1-%s-%s/costperuse", packageId, titleId);
    StringBuilder paramsSb = getEndpointParams(year, platform);
    return paramsSb.length() > 0
      ? baseUrl + "?" + paramsSb.toString()
      : baseUrl;
  }

  private String titleEndpoint(String packageId, String year, String platform) {
    String baseUrl = String.format("eholdings/titles/%s/costperuse", packageId);
    StringBuilder paramsSb = getEndpointParams(year, platform);
    return paramsSb.length() > 0
      ? baseUrl + "?" + paramsSb.toString()
      : baseUrl;
  }

  private String packageEndpoint(String packageId, String year, String platform) {
    String baseUrl = String.format("eholdings/packages/1-%s/costperuse", packageId);
    StringBuilder paramsSb = getEndpointParams(year, platform);
    return paramsSb.length() > 0
      ? baseUrl + "?" + paramsSb.toString()
      : baseUrl;
  }

  private String packageResourcesEndpoint(String packageId, String year, String platform, String page, String size) {
    String baseUrl = String.format("eholdings/packages/1-%s/resources/costperuse", packageId);
    StringBuilder paramsSb = getEndpointParams(year, platform, page, size);
    return paramsSb.length() > 0
      ? baseUrl + "?" + paramsSb.toString()
      : baseUrl;
  }

  private StringBuilder getEndpointParams(String year, String platform, String page, String size) {
    StringBuilder paramsSb = new StringBuilder();
    if (year != null) {
      paramsSb.append("fiscalYear=").append(year);
    }
    addParam(platform, paramsSb, "platform=");
    addParam(page, paramsSb, "page=");
    addParam(size, paramsSb, "size=");
    return paramsSb;
  }

  private void addParam(String platform, StringBuilder paramsSb, String s) {
    if (platform != null) {
      if (paramsSb.length() > 0) {
        paramsSb.append("&");
      }
      paramsSb.append(s).append(platform);
    }
  }

  private StringBuilder getEndpointParams(String year, String platform) {
    return getEndpointParams(year, platform, null, null);
  }

  private DbHoldingInfo generateHolding(String packageId, int titleId) {
    return DbHoldingInfo.builder()
      .packageId(packageId)
      .titleId(String.valueOf(titleId))
      .publicationTitle(random.nextObject(String.class))
      .publisherName(random.nextObject(String.class))
      .resourceType("Book")
      .vendorId("1")
      .build();
  }
}
