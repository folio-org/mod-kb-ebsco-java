package org.folio.rest.impl;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.http.HttpStatus;
import org.folio.config.cache.RMAPIConfigurationCache;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.util.RestConstants;
import org.folio.util.TestUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class EholdingsPackagesTest {

  private static final String STUB_CUSTOMER_ID = "TEST_CUSTOMER_ID";
  private static int port;
  private static String host;

  @org.junit.Rule
  public WireMockRule userMockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new ConsoleNotifier(true)));

  @BeforeClass
  public static void setUpClass(final TestContext context) {
    Vertx vertx = Vertx.vertx();
    vertx.exceptionHandler(context.exceptionHandler());
    port = NetworkUtils.nextFreePort();
    host = "http://localhost";

    DeploymentOptions restVerticleDeploymentOptions = new DeploymentOptions()
      .setConfig(new JsonObject().put("http.port", port));
    vertx.deployVerticle(RestVerticle.class.getName(), restVerticleDeploymentOptions,
      context.asyncAssertSuccess());

  }

  @Before
  public void setUp() {
    RMAPIConfigurationCache.getInstance().invalidate();
  }

  @Test
  public void shouldReturnPackagesOnGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/packages/get-packages-response.json";

    String wiremockUrl = host + ":" + userMockServer.port();
    TestUtil.mockConfiguration("responses/configuration/get-configuration.json", wiremockUrl);
    WireMock.stubFor(
      WireMock.get(
        new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/packages.*"),
          true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(stubResponseFile))));

    PackageCollection packages = RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/packages?q=American&filter[type]=abstractandindex&count=5")
      .then()
      .statusCode(HttpStatus.SC_OK).extract().as(PackageCollection.class);

    comparePackages(packages);
  }

  @Test
  public void shouldReturn400WhenCountInvalid() {
    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/packages?q=American&filter[type]=abstractandindex&count=500")
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  private void comparePackages(PackageCollection actual) {
    assertThat(actual.getMeta().getTotalResults(), equalTo(414));
    assertThat(actual.getData().get(0).getId(), equalTo("392-3007"));
    assertThat(actual.getData().get(0).getType(), equalTo("packages"));
    assertThat(actual.getData().get(0).getAttributes().getName(), equalTo("American Academy of Family Physicians"));
    assertThat(actual.getData().get(0).getAttributes().getPackageId(), equalTo(3007));
    assertThat(actual.getData().get(0).getAttributes().getIsCustom(), equalTo(false));
    assertThat(actual.getData().get(0).getAttributes().getProviderId(), equalTo(392));
    assertThat(actual.getData().get(0).getAttributes().getProviderName(), equalTo("American Academy of Family Physicians"));
    assertThat(actual.getData().get(0).getAttributes().getTitleCount(), equalTo(3));
    assertThat(actual.getData().get(0).getAttributes().getIsSelected(), equalTo(false));
    assertThat(actual.getData().get(0).getAttributes().getSelectedCount(), equalTo(0));
    assertThat(actual.getData().get(0).getAttributes().getContentType(), equalTo("E-Journal"));
    assertThat(actual.getData().get(0).getAttributes().getIsCustom(), equalTo(false));
    assertThat(actual.getData().get(0).getAttributes().getPackageType(), equalTo("Variable"));
    assertThat(actual.getData().get(0).getAttributes().getVisibilityData().getReason(), equalTo(""));
    assertThat(actual.getData().get(0).getAttributes().getVisibilityData().getIsHidden(), equalTo(false));
    assertThat(actual.getData().get(0).getAttributes().getCustomCoverage().getBeginCoverage(), equalTo(""));
    assertThat(actual.getData().get(0).getAttributes().getCustomCoverage().getEndCoverage(), equalTo(""));
    assertThat(actual.getData().get(0).getAttributes().getProxy().getId(), equalTo(""));
    assertThat(actual.getData().get(0).getAttributes().getProxy().getInherited(), equalTo(true));

  }

  private RequestSpecification getRequestSpecification() {
    return TestUtil.getRequestSpecificationBuilder(host + ":" + port)
      .addHeader(RestConstants.OKAPI_URL_HEADER, host + ":" + userMockServer.port())
      .build();
  }
}
