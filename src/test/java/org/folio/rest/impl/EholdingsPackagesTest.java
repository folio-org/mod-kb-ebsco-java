package org.folio.rest.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.PackageDataAttributes.ContentType;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.Token;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.PackageData;
import org.folio.util.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(VertxUnitRunner.class)
public class EholdingsPackagesTest extends WireMockTestBase {

  private static final String PACKAGE_BY_ID_STUB_FILE = "responses/rmapi/packages/get-package-by-id-response.json";
  private static final String CONFIGURATION_STUB_FILE = "responses/configuration/get-configuration.json";
  protected static final int STUB_PACKAGE_ID = 3964;
  protected static final int STUB_VENDOR_ID = 111111;

  @Test
  public void shouldReturnPackagesOnGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/packages/get-packages-response.json";

    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

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

    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    UrlPathPattern vendorsPattern = new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), true);
    UrlPathPattern packagesByProviderIdPattern = new UrlPathPattern(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages.*"), true);

    WireMock.stubFor(
      WireMock.get(vendorsPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(providerByCustIdStubResponseFile))));

    WireMock.stubFor(
      WireMock.get(packagesByProviderIdPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(packagesStubResponseFile))));

    PackageCollection packages = RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/packages?q=a&count=5&page=1&filter[custom]=true")
      .then()
      .statusCode(HttpStatus.SC_OK).extract().as(PackageCollection.class);

    comparePackages(packages, getExpectedPackageCollection());
  }

  @Test
  public void shouldReturnPackagesOnGetById() throws IOException, URISyntaxException {
    String packagesStubResponseFile = "responses/rmapi/packages/get-package-by-id-response.json";

    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration("responses/configuration/get-configuration.json", wiremockUrl);

    WireMock.stubFor(
      WireMock.get(
        new UrlPathPattern(new RegexPattern(
          "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID),
          true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(packagesStubResponseFile))));


    Package packageData = RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID)
      .then()
      .statusCode(HttpStatus.SC_OK).extract().as(Package.class);

    comparePackage(packageData, getExpectedPackage());
  }

  @Test
  public void shouldReturn404WhenPackageIsNotFoundOnRMAPI() throws IOException, URISyntaxException {
    String wiremockUrl = getWiremockUrl();
    TestUtil.mockConfiguration("responses/configuration/get-configuration.json", wiremockUrl);

    WireMock.stubFor(
      WireMock.get(
        new UrlPathPattern(new RegexPattern(
          "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID),
          true))
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(HttpStatus.SC_NOT_FOUND)));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .get("eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID)
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND);
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

  @Test
  public void shouldSendDeleteRequestForPackage() throws IOException, URISyntaxException {
    String providerId = "123355";
    String packageId = "2884739";

    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    UrlPathPattern packageUrlPattern = new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + providerId + "/packages/" + packageId), false);
    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern("{\"isSelected\":false}", true, true);

    WireMock.stubFor(
      WireMock.get(packageUrlPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(TestUtil.readFile(PACKAGE_BY_ID_STUB_FILE))));

    WireMock.stubFor(
      WireMock.put(packageUrlPattern)
        .withRequestBody(putBodyPattern)
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(HttpStatus.SC_NO_CONTENT)));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete("eholdings/packages/" + providerId + "-" + packageId)
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    WireMock.verify(1, WireMock.putRequestedFor(packageUrlPattern)
      .withRequestBody(putBodyPattern));
  }

  @Test
  public void shouldReturn400WhenPackageIdIsInvalid() {
    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete("eholdings/packages/abc-def")
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturn400WhenPackageIsNotCustom() throws URISyntaxException, IOException {
    String providerId = "123355";
    String packageId = "2884739";

    TestUtil.mockConfiguration(CONFIGURATION_STUB_FILE, getWiremockUrl());

    ObjectMapper mapper = new ObjectMapper();
    PackageData packageData = mapper.readValue(TestUtil.getFile(PACKAGE_BY_ID_STUB_FILE), PackageData.class);
    packageData.setCustom(false);

    WireMock.stubFor(
      WireMock.get(new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + providerId + "/packages/" + packageId), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(mapper.writeValueAsString(packageData))));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete("eholdings/packages/" + providerId + "-" + packageId)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  private void comparePackage(Package actual, Package expected) {
    assertThat(actual.getJsonapi().getVersion(), equalTo(expected.getJsonapi().getVersion()));
    comparePackageItem(actual.getData(), expected.getData());
  }

  private void comparePackages(PackageCollection actual, PackageCollection expected) {
    assertThat(actual.getMeta().getTotalResults(), equalTo(expected.getMeta().getTotalResults()));
    PackageCollectionItem actualItem = actual.getData().get(0);
    PackageCollectionItem expectedItem = expected.getData().get(0);
    comparePackageItem(actualItem, expectedItem);

  }

  private void comparePackageItem(PackageCollectionItem actualItem, PackageCollectionItem expectedItem) {
    assertThat(actualItem.getId(), equalTo(expectedItem.getId()));
    assertThat(actualItem.getType(), equalTo("packages"));
    assertThat(actualItem.getAttributes().getName(),
      equalTo(expectedItem.getAttributes().getName()));
    assertThat(actualItem.getAttributes().getPackageId(),
      equalTo(expectedItem.getAttributes().getPackageId()));
    assertThat(actualItem.getAttributes().getIsCustom(),
      equalTo(expectedItem.getAttributes().getIsCustom()));
    assertThat(actualItem.getAttributes().getProviderId(),
      equalTo(expectedItem.getAttributes().getProviderId()));
    assertThat(actualItem.getAttributes().getProviderName(),
      equalTo(expectedItem.getAttributes().getProviderName()));
    assertThat(actualItem.getAttributes().getTitleCount(),
      equalTo(expectedItem.getAttributes().getTitleCount()));
    assertThat(actualItem.getAttributes().getIsSelected(),
      equalTo(expectedItem.getAttributes().getIsSelected()));
    assertThat(actualItem.getAttributes().getSelectedCount(),
      equalTo(expectedItem.getAttributes().getSelectedCount()));
    assertThat(actualItem.getAttributes().getContentType().value(),
      equalTo(expectedItem.getAttributes().getContentType().value()));
    assertThat(actualItem.getAttributes().getIsCustom(),
      equalTo(expectedItem.getAttributes().getIsCustom()));
    assertThat(actualItem.getAttributes().getPackageType(),
      equalTo(expectedItem.getAttributes().getPackageType()));
    assertThat(actualItem.getAttributes().getVisibilityData().getReason(),
      equalTo(expectedItem.getAttributes().getVisibilityData().getReason()));
    assertThat(actualItem.getAttributes().getVisibilityData().getIsHidden(),
      equalTo(expectedItem.getAttributes().getVisibilityData().getIsHidden()));
    assertThat(actualItem.getAttributes().getCustomCoverage().getBeginCoverage(),
      equalTo(expectedItem.getAttributes().getCustomCoverage().getBeginCoverage()));
    assertThat(actualItem.getAttributes().getCustomCoverage().getEndCoverage(),
      equalTo(expectedItem.getAttributes().getCustomCoverage().getEndCoverage()));
    assertThat(actualItem.getAttributes().getAllowKbToAddTitles(),
      equalTo(expectedItem.getAttributes().getAllowKbToAddTitles()));
  }

  private PackageCollection getExpectedPackageCollection() {
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

  private Package getExpectedPackage() {
    return new Package().withData(new PackageCollectionItem()
      .withId("111111-3964")
      .withType("packages")
      .withAttributes(new PackageDataAttributes()
        .withName("carole and sams test")
        .withPackageId(3964)
        .withIsCustom(true)
        .withProviderId(111111)
        .withProviderName("APIDEV CORPORATE CUSTOMER")
        .withTitleCount(6)
        .withIsSelected(true)
        .withSelectedCount(6)
        .withPackageType("Custom")
        .withContentType(ContentType.UNKNOWN)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")
          .withEndCoverage(""))
        .withVisibilityData(new VisibilityData()
          .withIsHidden(false)
          .withReason("")
        )
      .withProxy(new Proxy()
        .withId("<n>")
        .withInherited(true))
      .withAllowKbToAddTitles(false)
      .withPackageToken(new Token()
        .withFactName("[[gale.customcode.infocust]]")
        .withHelpText("help text")
        .withValue("token value")
        .withPrompt("res_id=info:sid/gale:")
        )))
    .withJsonapi(RestConstants.JSONAPI);
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
