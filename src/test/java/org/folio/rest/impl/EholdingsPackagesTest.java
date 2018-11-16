package org.folio.rest.impl;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.PackageDataAttributes.ContentType;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.folio.util.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class EholdingsPackagesTest extends WireMockTestBase {

  protected static final int STUB_VENDOR_ID = 111111;

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

    comparePackages(packages, getExpectedCollectionPackageItem());
  }

  @Test
  public void shouldReturnPackagesOnGetWithPackageId() throws IOException, URISyntaxException {
    String packagesStubResponseFile = "responses/rmapi/packages/get-packages-by-provider-id.json";
    String providerByCustIdStubResponseFile = "responses/rmapi/packages/get-package-provider-by-id.json";

    String wiremockUrl = host + ":" + userMockServer.port();
    TestUtil.mockConfiguration("responses/configuration/get-configuration.json", wiremockUrl);

    WireMock.stubFor(
      WireMock.get(
        new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"),
          true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(providerByCustIdStubResponseFile))));

    WireMock.stubFor(
      WireMock.get(
        new UrlPathPattern(new RegexPattern(
          "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages.*"),
          true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(packagesStubResponseFile))));

    PackageCollection packages = RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/packages?q=a&count=5&page=1&filter[custom]=true")
      .then()
      .statusCode(HttpStatus.SC_OK).extract().as(PackageCollection.class);

    comparePackages(packages, getExpectedPackage());
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

  private void comparePackages(PackageCollection actual, PackageCollection expected) {
    assertThat(actual.getMeta().getTotalResults(), equalTo(expected.getMeta().getTotalResults()));
    assertThat(actual.getData().get(0).getId(), equalTo(expected.getData().get(0).getId()));
    assertThat(actual.getData().get(0).getType(), equalTo("packages"));
    assertThat(actual.getData().get(0).getAttributes().getName(),
      equalTo(expected.getData().get(0).getAttributes().getName()));
    assertThat(actual.getData().get(0).getAttributes().getPackageId(),
      equalTo(expected.getData().get(0).getAttributes().getPackageId()));
    assertThat(actual.getData().get(0).getAttributes().getIsCustom(),
      equalTo(expected.getData().get(0).getAttributes().getIsCustom()));
    assertThat(actual.getData().get(0).getAttributes().getProviderId(),
      equalTo(expected.getData().get(0).getAttributes().getProviderId()));
    assertThat(actual.getData().get(0).getAttributes().getProviderName(),
      equalTo(expected.getData().get(0).getAttributes().getProviderName()));
    assertThat(actual.getData().get(0).getAttributes().getTitleCount(),
      equalTo(expected.getData().get(0).getAttributes().getTitleCount()));
    assertThat(actual.getData().get(0).getAttributes().getIsSelected(),
      equalTo(expected.getData().get(0).getAttributes().getIsSelected()));
    assertThat(actual.getData().get(0).getAttributes().getSelectedCount(),
      equalTo(expected.getData().get(0).getAttributes().getSelectedCount()));
    assertThat(actual.getData().get(0).getAttributes().getContentType().value(),
      equalTo(expected.getData().get(0).getAttributes().getContentType().value()));
    assertThat(actual.getData().get(0).getAttributes().getIsCustom(),
      equalTo(expected.getData().get(0).getAttributes().getIsCustom()));
    assertThat(actual.getData().get(0).getAttributes().getPackageType(),
      equalTo(expected.getData().get(0).getAttributes().getPackageType()));
    assertThat(actual.getData().get(0).getAttributes().getVisibilityData().getReason(),
      equalTo(expected.getData().get(0).getAttributes().getVisibilityData().getReason()));
    assertThat(actual.getData().get(0).getAttributes().getVisibilityData().getIsHidden(),
      equalTo(expected.getData().get(0).getAttributes().getVisibilityData().getIsHidden()));
    assertThat(actual.getData().get(0).getAttributes().getCustomCoverage().getBeginCoverage(),
      equalTo(expected.getData().get(0).getAttributes().getCustomCoverage().getBeginCoverage()));
    assertThat(actual.getData().get(0).getAttributes().getCustomCoverage().getEndCoverage(),
      equalTo(expected.getData().get(0).getAttributes().getCustomCoverage().getEndCoverage()));

  }

  private PackageCollection getExpectedPackage() {
    List<PackageCollectionItem> collectionItems = new ArrayList<>();
    PackageCollectionItem collectionItem = new PackageCollectionItem()
      .withId("1111111-2222222")
      .withAttributes(new PackageDataAttributes()
        .withName("TEST_PACKAGE_NAME")
        .withPackageId(2222222)
        .withIsCustom(true)
        .withProviderId(1111111)
        .withProviderName("TEST_VENDOR_NAME")
        .withTitleCount(5)
        .withIsSelected(true)
        .withSelectedCount(5)
        .withPackageType("Custom")
        .withContentType(ContentType.ONLINE_REFERENCE)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")
          .withEndCoverage(""))
        .withVisibilityData(new VisibilityData()
          .withIsHidden(false)
          .withReason("")
        ));
    collectionItems.add(collectionItem);
    return new PackageCollection().withData(collectionItems)
      .withMeta(new MetaTotalResults().withTotalResults(1));

  }

  private PackageCollection getExpectedCollectionPackageItem() {
    List<PackageCollectionItem> collectionItems = new ArrayList<>();
    PackageCollectionItem collectionItem = new PackageCollectionItem()
      .withId("392-3007")
      .withAttributes(new PackageDataAttributes()
        .withName("American Academy of Family Physicians")
        .withPackageId(3007)
        .withIsCustom(false)
        .withProviderId(392)
        .withProviderName("American Academy of Family Physicians")
        .withTitleCount(3)
        .withIsSelected(false)
        .withSelectedCount(0)
        .withPackageType("Variable")
        .withContentType(ContentType.E_JOURNAL)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")
          .withEndCoverage(""))
        .withVisibilityData(new VisibilityData()
          .withIsHidden(false)
          .withReason("")
        ));
    collectionItems.add(collectionItem);
    return new PackageCollection().withData(collectionItems)
      .withMeta(new MetaTotalResults().withTotalResults(414));

  }
}
