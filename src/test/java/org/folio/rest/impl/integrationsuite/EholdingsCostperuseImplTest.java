package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.junit.Assert.assertEquals;

import static org.folio.repository.kbcredentials.KbCredentialsTableConstants.KB_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UCCredentialsTableConstants.UC_CREDENTIALS_TABLE_NAME;
import static org.folio.repository.uc.UCSettingsTableConstants.UC_SETTINGS_TABLE_NAME;
import static org.folio.util.AssertTestUtil.assertErrorContainsDetail;
import static org.folio.util.AssertTestUtil.assertErrorContainsTitle;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KbCredentialsTestUtil.STUB_CREDENTIALS_NAME;
import static org.folio.util.KbCredentialsTestUtil.saveKbCredentials;
import static org.folio.util.UCCredentialsTestUtil.setUpUCCredentials;
import static org.folio.util.UCSettingsTestUtil.saveUCSettings;
import static org.folio.util.UCSettingsTestUtil.stubSettings;

import java.io.IOException;
import java.net.URISyntaxException;

import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
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
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.ResourceCostPerUse;
import org.folio.rest.jaxrs.model.TitleCostPerUse;
import org.folio.test.util.TestUtil;

@RunWith(VertxUnitRunner.class)
public class EholdingsCostperuseImplTest extends WireMockTestBase {

  @Autowired
  private UCAuthEbscoClient authEbscoClient;
  @Autowired
  private UCApigeeEbscoClient apigeeEbscoClient;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    String credentialsId = saveKbCredentials(getWiremockUrl(), STUB_CREDENTIALS_NAME, STUB_API_KEY, STUB_CUSTOMER_ID, vertx);
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
    String stubApigeeGetTitlePackageResponseFile = "responses/uc/titles/get-title-packages-cost-per-use-response.json";
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

  private StringBuilder getEndpointParams(String year, String platform) {
    StringBuilder paramsSb = new StringBuilder();
    if (year != null) {
      paramsSb.append("fiscalYear=").append(year);
    }
    if (platform != null) {
      if (paramsSb.length() > 0) {
        paramsSb.append("&");
      }
      paramsSb.append("platform=").append(platform);
    }
    return paramsSb;
  }
}
