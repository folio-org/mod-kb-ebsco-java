package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.folio.repository.packages.PackageTableConstants.PACKAGES_TABLE_NAME;
import static org.folio.repository.resources.ResourceTableConstants.RESOURCES_TABLE_NAME;
import static org.folio.rest.impl.PackagesTestData.FULL_PACKAGE_ID;
import static org.folio.rest.impl.PackagesTestData.FULL_PACKAGE_ID_2;
import static org.folio.rest.impl.PackagesTestData.FULL_PACKAGE_ID_3;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_CONTENT_TYPE;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_ID;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_NAME;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_NAME_2;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID;
import static org.folio.rest.impl.ResourcesTestData.STUB_MANAGED_RESOURCE_ID;
import static org.folio.rest.impl.TagsTestData.STUB_TAG_VALUE;
import static org.folio.rest.impl.TagsTestData.STUB_TAG_VALUE_2;
import static org.folio.rest.impl.TagsTestData.STUB_TAG_VALUE_3;
import static org.folio.util.PackagesTestUtil.buildDbPackage;
import static org.folio.util.PackagesTestUtil.setUpPackages;
import static org.folio.util.TestUtil.getFile;
import static org.folio.util.TestUtil.mockDefaultConfiguration;
import static org.folio.util.TestUtil.mockGet;
import static org.folio.util.TestUtil.mockPost;
import static org.folio.util.TestUtil.mockPut;
import static org.folio.util.TestUtil.readFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.restassured.RestAssured;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageData;
import org.folio.repository.RecordType;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.util.PackagesTestUtil;
import org.folio.util.ResourcesTestUtil;
import org.folio.util.TagsTestUtil;
import org.folio.util.TestUtil;

@RunWith(VertxUnitRunner.class)
public class EholdingsPackagesTest extends WireMockTestBase {

  private static final String PACKAGE_STUB_FILE = "responses/rmapi/packages/get-package-by-id-response.json";
  private static final String CUSTOM_PACKAGE_STUB_FILE = "responses/rmapi/packages/get-custom-package-by-id-response.json";
  private static final String RESOURCES_BY_PACKAGE_ID_STUB_FILE = "responses/rmapi/resources/get-resources-by-package-id-response.json";
  private static final String EXPECTED_PACKAGE_BY_ID_STUB_FILE = "responses/kb-ebsco/packages/expected-package-by-id.json";
  private static final String EXPECTED_RESOURCES_STUB_FILE = "responses/kb-ebsco/resources/expected-resources-by-package-id.json";
  private static final String EXPECTED_RESOURCES_WITH_TAGS_STUB_FILE = "responses/kb-ebsco/resources/expected-resources-by-package-id-with-tags.json";
  private static final String VENDOR_BY_PACKAGE_ID_STUB_FILE = "responses/rmapi/vendors/get-vendor-by-id-for-package.json";

  public static final String PACKAGES_STUB_URL = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages";

  private static final String PACKAGES_PATH = "eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID;
  private static final String PACKAGE_BY_ID_URL = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID;
  private static final UrlPathPattern PACKAGE_URL_PATTERN = new UrlPathPattern(new EqualToPattern(PACKAGE_BY_ID_URL), false);
  private static final String RESOURCES_BY_PACKAGE_ID_URL = PACKAGE_BY_ID_URL + "/titles";
  private static final String PACKAGED_UPDATED_STATE = "Packaged updated";
  private static final String GET_PACKAGE_SCENARIO = "Get package";
  private static final String STUB_TITLE_NAME = "Activity Theory Perspectives on Technology in Higher Education";

  @Test
  public void shouldReturnPackagesOnGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/packages/get-packages-response.json";

    mockDefaultConfiguration(getWiremockUrl());
    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/packages.*"), stubResponseFile);

    String packages = getWithOk("eholdings/packages?q=American&filter[type]=abstractandindex&count=5")
      .asString();
    JSONAssert.assertEquals(readFile("responses/kb-ebsco/packages/expected-package-collection-with-five-elements.json"),
      packages, false);
  }


  @Test
  public void shouldReturnPackagesOnSearchByTagsOnly() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, RecordType.PACKAGE, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID_2, RecordType.PACKAGE, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID_2, RecordType.PACKAGE, STUB_TAG_VALUE_2);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID_3, RecordType.PACKAGE, STUB_TAG_VALUE_3);

      setUpPackages(vertx, getWiremockUrl());

      PackageCollection packageCollection = getWithOk(
        "eholdings/packages?filter[tags]=" + STUB_TAG_VALUE + "," + STUB_TAG_VALUE_2).as(PackageCollection.class);
      List<PackageCollectionItem> packages = packageCollection.getData();

      assertEquals(2, (int) packageCollection.getMeta().getTotalResults());
      assertEquals(2, packages.size());
      assertEquals(STUB_PACKAGE_NAME, packages.get(0).getAttributes().getName());
      assertEquals(STUB_PACKAGE_NAME_2, packages.get(1).getAttributes().getName());
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx,PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturnEmptyResponseWhenPackagesReturnedWithErrorOnSearchByTags() throws IOException, URISyntaxException {
    try {
      PackagesTestUtil.addPackage(vertx, buildDbPackage(FULL_PACKAGE_ID, STUB_PACKAGE_NAME));
      PackagesTestUtil.addPackage(vertx, buildDbPackage(FULL_PACKAGE_ID_2, STUB_PACKAGE_NAME_2));
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, RecordType.PACKAGE, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID_2, RecordType.PACKAGE, STUB_TAG_VALUE);

      mockDefaultConfiguration(getWiremockUrl());

      mockGet(new RegexPattern(".*vendors/.*/packages/.*"), HttpStatus.SC_INTERNAL_SERVER_ERROR);

      PackageCollection packageCollection = getWithOk("eholdings/packages?filter[tags]=" + STUB_TAG_VALUE).as(PackageCollection.class);
      List<PackageCollectionItem> packages = packageCollection.getData();

      assertEquals(2, (int) packageCollection.getMeta().getTotalResults());
      assertEquals(0, packages.size());
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx,PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturnPackagesOnSearchWithPagination() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, RecordType.PACKAGE, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID_2, RecordType.PACKAGE, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID_3, RecordType.PACKAGE, STUB_TAG_VALUE);

      setUpPackages(vertx, getWiremockUrl());

      PackageCollection packageCollection = getWithOk(
        "eholdings/packages?page=2&count=1&filter[tags]=" + STUB_TAG_VALUE).as(PackageCollection.class);

      List<PackageCollectionItem> packages = packageCollection.getData();

      assertEquals(3, (int) packageCollection.getMeta().getTotalResults());
      assertEquals(1, packages.size());
      assertEquals(STUB_PACKAGE_NAME_2, packages.get(0).getAttributes().getName());
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx,PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturnPackagesOnGetWithPackageId() throws IOException, URISyntaxException {
    String packagesStubResponseFile = "responses/rmapi/packages/get-packages-by-provider-id.json";
    String providerIdByCustIdStubResponseFile = "responses/rmapi/proxiescustomlabels/get-root-proxy-custom-labels-success-response.json";

    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), providerIdByCustIdStubResponseFile);
    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages.*"),
      packagesStubResponseFile);

    String packages = getWithOk("eholdings/packages?q=a&count=5&page=1&filter[custom]=true")
      .asString();

    JSONAssert.assertEquals(readFile("responses/kb-ebsco/packages/expected-package-collection-with-one-element.json"),
      packages, false);
}

  @Test
  public void shouldReturnPackagesOnGetById() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);

    String packageData = getWithOk(PACKAGES_PATH).asString();

    JSONAssert.assertEquals(readFile(EXPECTED_PACKAGE_BY_ID_STUB_FILE), packageData, false);
  }

  @Test
  public void shouldReturnPackageWithTagOnGetById() throws IOException, URISyntaxException {
    try {
      String packageId = FULL_PACKAGE_ID;
      TagsTestUtil.insertTag(vertx, packageId, RecordType.PACKAGE, STUB_TAG_VALUE);
      mockDefaultConfiguration(getWiremockUrl());

      mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);

      Package packageData = getWithOk("eholdings/packages/" + packageId).as(Package.class);

      assertTrue(packageData.getData().getAttributes().getTags().getTagList().contains(STUB_TAG_VALUE));
    }
    finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx,PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldDeleteAndAddPackageTagsOnPut() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, RecordType.PACKAGE, "test one");
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutWithTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PACKAGE);
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx,PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldAddPackageTagsOnPutWhenPackageAlreadyHasTags() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, RecordType.PACKAGE, STUB_TAG_VALUE);
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutWithTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PACKAGE);
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx, PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldAddPackageTagsOnPostWhenPackageAlreadyHasTags() throws IOException, URISyntaxException {
    try {
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutWithTags(Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2));
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PACKAGE);
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx,PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldAddPackageDataOnPut() throws IOException, URISyntaxException {
    try {
      List<String> tags = Collections.singletonList(STUB_TAG_VALUE);
      sendPutWithTags(tags);
      List<PackagesTestUtil.DbPackage> packages = PackagesTestUtil.getPackages(vertx);
      assertEquals(1, packages.size());
      assertEquals(FULL_PACKAGE_ID, packages.get(0).getId());
      assertEquals(STUB_PACKAGE_NAME, packages.get(0).getName());
      assertEquals(STUB_PACKAGE_CONTENT_TYPE, packages.get(0).getContentType());
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx,PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldUpdatePackageDataOnSecondPut() throws IOException, URISyntaxException {
    try {
      String newName = "new name";
      String newType = "newType";

      List<String> tags = Collections.singletonList(STUB_TAG_VALUE);
      sendPutWithTags(tags);

      ObjectMapper mapper = new ObjectMapper();
      PackageByIdData updatedPackage = mapper.readValue(readFile(PACKAGE_STUB_FILE), PackageByIdData.class)
        .toByIdBuilder().packageName(newName).contentType(newType).build();
      sendPutWithTags(mapper.writeValueAsString(updatedPackage), tags);

      List<PackagesTestUtil.DbPackage> packages = PackagesTestUtil.getPackages(vertx);
      assertEquals(1, packages.size());
      assertEquals(FULL_PACKAGE_ID, packages.get(0).getId());
      assertEquals(newName, packages.get(0).getName());
      assertEquals(newType, packages.get(0).getContentType());
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx,PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldDeletePackageDataOnPutWithEmptyTagList() throws IOException, URISyntaxException {
    List<String> tags = Collections.singletonList(STUB_TAG_VALUE);
    sendPutWithTags(tags);
    sendPutWithTags(Collections.emptyList());

    List<PackagesTestUtil.DbPackage> packages = PackagesTestUtil.getPackages(vertx);
    assertThat(packages, is(empty()));
  }

  @Test
  public void shouldDeletePackageDataAndTagsOnPutWithCustomPackageAndSelectedFalse() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, RecordType.PACKAGE, STUB_TAG_VALUE);
      PackagesTestUtil.addPackage(vertx, buildDbPackage(FULL_PACKAGE_ID, STUB_PACKAGE_NAME));
      ObjectMapper mapper = new ObjectMapper();

      UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern(PACKAGE_BY_ID_URL), false);
      mockDefaultConfiguration(getWiremockUrl());
      mockUpdateWithDeleteScenario(urlPattern, readFile(CUSTOM_PACKAGE_STUB_FILE));

      PackagePutRequest packageToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/package/put-package-custom-not-selected.json"), PackagePutRequest.class);

      putWithStatus(PACKAGES_PATH, mapper.writeValueAsString(packageToBeUpdated), SC_NOT_FOUND);

      List<PackagesTestUtil.DbPackage> packages = PackagesTestUtil.getPackages(vertx);
      List<String> packageTags = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PACKAGE);
      assertThat(packages, is(empty()));
      assertThat(packageTags, is(empty()));
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx,PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldAddPackageDataOnPost() throws URISyntaxException, IOException {
    try {
      String packagePostStubRequestFile = "requests/kb-ebsco/package/post-package-request.json";
      sendPostWithTags(readFile(packagePostStubRequestFile), Collections.singletonList(STUB_TAG_VALUE));
      List<PackagesTestUtil.DbPackage> packages = PackagesTestUtil.getPackages(vertx);
      assertEquals(1, packages.size());
      assertEquals(FULL_PACKAGE_ID, packages.get(0).getId());
      assertEquals(STUB_PACKAGE_NAME, packages.get(0).getName());
      assertEquals(STUB_PACKAGE_CONTENT_TYPE, packages.get(0).getContentType());
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx,PACKAGES_TABLE_NAME);
    }
  }


  @Test
  public void shouldDeleteAllPackageTagsOnPutWhenRequestHasEmptyListOfTags() throws IOException, URISyntaxException {
    TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, RecordType.PACKAGE, "test one");
      sendPutWithTags(Collections.emptyList());
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PACKAGE);
      assertThat(tagsAfterRequest, empty());
  }

  @Test
  public void shouldDoNothingOnPutWhenRequestHasNotTags() throws IOException, URISyntaxException {
    sendPutWithTags(null);
    sendPutWithTags(null);
    List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PACKAGE);
    assertThat(tagsAfterRequest, empty());
  }

  @Test
  public void shouldDeletePackageTagsOnDelete() throws IOException, URISyntaxException {
    TagsTestUtil.insertTag(vertx, FULL_PACKAGE_ID, RecordType.PACKAGE, "test one");

    mockDefaultConfiguration(getWiremockUrl());
    mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);

    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern("{\"isSelected\":false}", true, true);
    mockPut(new EqualToPattern(PACKAGE_BY_ID_URL), putBodyPattern, SC_NO_CONTENT);

    deleteWithOk(PACKAGES_PATH);

    List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PACKAGE);
    assertThat(tagsAfterRequest, empty());
  }

  @Test
  public void shouldDeletePackageOnDeleteRequest() throws IOException, URISyntaxException {
    sendPostWithTags(readFile("requests/kb-ebsco/package/post-package-request.json"), Collections.singletonList(STUB_TAG_VALUE));

    mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);
    mockPut(new EqualToPattern(PACKAGE_BY_ID_URL), new AnythingPattern(), SC_NO_CONTENT);

    deleteWithOk(PACKAGES_PATH);

    List<PackagesTestUtil.DbPackage> packages = PackagesTestUtil.getPackages(vertx);
    assertThat(packages, is(empty()));
  }

  @Test
  public void shouldReturn404WhenPackageIsNotFoundOnRMAPI() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), SC_NOT_FOUND);

    getWithStatus(PACKAGES_PATH, SC_NOT_FOUND);
  }

  @Test
  public void shouldReturnResourcesWhenIncludedFlagIsSetToResources() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());
    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);
    mockResourceById(RESOURCES_BY_PACKAGE_ID_STUB_FILE);

    Package packageData = getWithOk(PACKAGES_PATH + "?include=resources")
      .as(Package.class);

    ObjectMapper mapper = new ObjectMapper();
    Package expectedPackage = mapper.readValue(readFile(EXPECTED_PACKAGE_BY_ID_STUB_FILE), Package.class);
    ResourceCollection expectedResources = mapper.readValue(readFile(EXPECTED_RESOURCES_STUB_FILE), ResourceCollection.class);
    expectedPackage.getIncluded().addAll(expectedResources.getData());

    JSONAssert.assertEquals(mapper.writeValueAsString(expectedPackage), mapper.writeValueAsString(packageData), false);
  }

  @Test
  public void shouldReturnProviderWhenIncludedFlagIsSetToProvider() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new RegexPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);
    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID),
      VENDOR_BY_PACKAGE_ID_STUB_FILE);

    String actual = getWithOk(PACKAGES_PATH + "?include=provider").asString();

    String expected = readFile("responses/kb-ebsco/packages/expected-package-by-id-with-provider.json");
    JSONAssert.assertEquals(expected, actual, false);
  }

  @Test
  public void shouldReturn400WhenCountInvalid() {
    checkResponseNotEmptyWhenStatusIs400("eholdings/packages?q=American&filter[type]=abstractandindex&count=500");
  }

  @Test
  public void shouldSendDeleteRequestForPackage() throws IOException, URISyntaxException {

    mockDefaultConfiguration(getWiremockUrl());

    UrlPathPattern packageUrlPattern = new UrlPathPattern(new EqualToPattern(PACKAGE_BY_ID_URL), false);
    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern("{\"isSelected\":false}", true, true);

    mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), CUSTOM_PACKAGE_STUB_FILE);

    mockPut(new EqualToPattern(PACKAGE_BY_ID_URL), putBodyPattern, SC_NO_CONTENT);

    deleteWithOk(PACKAGES_PATH);

    verify(1, putRequestedFor(packageUrlPattern)
      .withRequestBody(putBodyPattern));
  }

  @Test
  public void shouldReturn400WhenPackageIdIsInvalid() {
    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete("eholdings/packages/abc-def")
      .then()
      .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturn400WhenPackageIsNotCustom() throws URISyntaxException, IOException {
    mockDefaultConfiguration(getWiremockUrl());

    ObjectMapper mapper = new ObjectMapper();
    PackageData packageData = mapper.readValue(getFile(CUSTOM_PACKAGE_STUB_FILE), PackageData.class)
      .toBuilder().isCustom(false).build();

    stubFor(
      get(new UrlPathPattern(new EqualToPattern(PACKAGE_BY_ID_URL), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(mapper.writeValueAsString(packageData))));

    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete(PACKAGES_PATH)
      .then()
      .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturn200WhenSelectingPackage() throws URISyntaxException, IOException {
    boolean updatedIsSelected = true;

    mockDefaultConfiguration(getWiremockUrl());

    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern(PACKAGE_BY_ID_URL), false);
    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern(readFile("requests/rmapi/packages/put-package-is-selected.json"), true, true);

    ObjectMapper mapper = new ObjectMapper();
    PackageByIdData packageData = mapper.readValue(getFile(PACKAGE_STUB_FILE), PackageByIdData.class);
    packageData = packageData.toByIdBuilder().isSelected(updatedIsSelected).build();
    String updatedPackageValue = mapper.writeValueAsString(packageData);
    mockUpdateScenario(urlPattern, readFile(PACKAGE_STUB_FILE), updatedPackageValue);

    Package aPackage = putWithOk(PACKAGES_PATH, readFile("requests/kb-ebsco/package/put-package-selected.json"))
      .as(Package.class);

    assertEquals(updatedIsSelected, aPackage.getData().getAttributes().getIsSelected());

    verify(putRequestedFor(urlPattern)
      .withRequestBody(putBodyPattern));
  }

  @Test
  public void shouldUpdateAllAttributesInSelectedPackage() throws URISyntaxException, IOException {
    boolean updatedSelected = true;
    boolean updatedAllowEbscoToAddTitles = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";

    mockDefaultConfiguration(getWiremockUrl());

    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern(PACKAGE_BY_ID_URL), false);
    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern(readFile("requests/rmapi/packages/put-package-is-selected-multiple-attributes.json"), true, true);

    ObjectMapper mapper = new ObjectMapper();
    PackageByIdData packageData = mapper.readValue(getFile(PACKAGE_STUB_FILE), PackageByIdData.class);

    packageData = packageData.toByIdBuilder()
      .isSelected(updatedSelected)
      .customCoverage(CoverageDates.builder()
        .beginCoverage(updatedBeginCoverage)
        .endCoverage(updatedEndCoverage)
        .build())
      .allowEbscoToAddTitles(updatedAllowEbscoToAddTitles)
      .visibilityData(packageData.getVisibilityData().toBuilder().isHidden(updatedHidden).build())
      .build();

    String updatedPackageValue = mapper.writeValueAsString(packageData);
    mockUpdateScenario(urlPattern, readFile(PACKAGE_STUB_FILE), updatedPackageValue);

    Package aPackage = putWithOk(
      PACKAGES_PATH,
      readFile("requests/kb-ebsco/package/put-package-selected-multiple-attributes")).as(Package.class);

    verify(putRequestedFor(urlPattern)
      .withRequestBody(putBodyPattern));

    assertEquals(updatedSelected, aPackage.getData().getAttributes().getIsSelected());
    assertEquals(updatedAllowEbscoToAddTitles, aPackage.getData().getAttributes().getAllowKbToAddTitles());
    assertEquals(updatedHidden, aPackage.getData().getAttributes().getVisibilityData().getIsHidden());
    assertEquals(updatedBeginCoverage, aPackage.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, aPackage.getData().getAttributes().getCustomCoverage().getEndCoverage());
  }

  @Test
  public void shouldUpdateAllAttributesInCustomPackage() throws URISyntaxException, IOException {
    boolean updatedSelected = true;
    boolean updatedHidden = true;
    String updatedBeginCoverage = "2003-01-01";
    String updatedEndCoverage = "2004-01-01";
    String updatedPackageName = "name of the ages forever and ever";

    mockDefaultConfiguration(getWiremockUrl());

    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern(readFile("requests/rmapi/packages/put-package-custom.json"), true, true);

    ObjectMapper mapper = new ObjectMapper();
    PackageByIdData packageData = mapper.readValue(getFile(CUSTOM_PACKAGE_STUB_FILE), PackageByIdData.class);

    packageData = packageData.toByIdBuilder()
      .isSelected(updatedSelected)
      .visibilityData(packageData.getVisibilityData().toBuilder().isHidden(updatedHidden).build())
      .customCoverage(CoverageDates.builder()
        .beginCoverage(updatedBeginCoverage)
        .endCoverage(updatedEndCoverage)
        .build())
      .packageName(updatedPackageName)
      .contentType("AggregatedFullText").build();

    String updatedPackageValue = mapper.writeValueAsString(packageData);
    mockUpdateScenario(PACKAGE_URL_PATTERN, readFile(CUSTOM_PACKAGE_STUB_FILE), updatedPackageValue);

    Package aPackage = putWithOk(
      PACKAGES_PATH,
      readFile("requests/kb-ebsco/package/put-package-custom-multiple-attributes")).as(Package.class);

    verify(putRequestedFor(PACKAGE_URL_PATTERN)
      .withRequestBody(putBodyPattern));

    assertEquals(updatedSelected, aPackage.getData().getAttributes().getIsSelected());
    assertEquals(updatedHidden, aPackage.getData().getAttributes().getVisibilityData().getIsHidden());
    assertEquals(updatedBeginCoverage, aPackage.getData().getAttributes().getCustomCoverage().getBeginCoverage());
    assertEquals(updatedEndCoverage, aPackage.getData().getAttributes().getCustomCoverage().getEndCoverage());
    assertEquals(updatedPackageName, aPackage.getData().getAttributes().getName());
    assertEquals(ContentType.AGGREGATED_FULL_TEXT, aPackage.getData().getAttributes().getContentType());
  }

  @Test
  public void shouldUpdateOnlyTagsWhenPackageIsNotSelected() throws URISyntaxException, IOException {
    try {
      mockDefaultConfiguration(getWiremockUrl());
      ObjectMapper mapper = new ObjectMapper();
      PackagePutRequest request = mapper.readValue(
        readFile("requests/kb-ebsco/package/put-package-not-selected-non-empty-fields.json"), PackagePutRequest.class);
      List<String> addedTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      request.getData().getAttributes().withTags(new Tags().withTagList(addedTags));

      mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), PACKAGE_STUB_FILE);

      putWithOk(PACKAGES_PATH, mapper.writeValueAsString(request));

      List<String> tags = TagsTestUtil.getTagsForRecordType(vertx, RecordType.PACKAGE);
      assertThat(tags, containsInAnyOrder(addedTags.toArray()));
      WireMock.verify(0, putRequestedFor(anyUrl()));
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx,PACKAGES_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturn400WhenRMAPIReturns400() throws URISyntaxException, IOException {
    EqualToPattern urlPattern = new EqualToPattern(
      "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/"
        + STUB_PACKAGE_ID);

    mockDefaultConfiguration(getWiremockUrl());

    mockGet(urlPattern, PACKAGE_STUB_FILE);

    mockPut(urlPattern, SC_BAD_REQUEST);

    putWithStatus(PACKAGES_PATH, readFile("requests/kb-ebsco/package/put-package-selected.json"), SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturn200WhenPackagePostIsValid() throws URISyntaxException, IOException {
    String packagePostStubRequestFile = "requests/kb-ebsco/package/post-package-request.json";
    String packagePostRMAPIRequestFile = "requests/rmapi/packages/post-package.json";
    String actual = sendPostWithTags(readFile(packagePostStubRequestFile), Collections.emptyList());

    String packagePostStubResponseFile = "responses/kb-ebsco/packages/get-created-package-response.json";
    String expected = readFile(packagePostStubResponseFile);

    JSONAssert.assertEquals(expected, actual, false);
    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern(readFile(packagePostRMAPIRequestFile), false, true);
    verify(1, postRequestedFor(new UrlPathPattern(new EqualToPattern(PACKAGES_STUB_URL), false))
      .withRequestBody(postBodyPattern));
  }

  @Test
  public void shouldReturn400WhenPackagePostDataIsInvalid()throws URISyntaxException, IOException{
    String providerStubResponseFile = "responses/rmapi/packages/get-package-provider-by-id.json";
    String packagePostStubRequestFile = "requests/kb-ebsco/package/post-package-request.json";
    String response = "responses/rmapi/packages/post-package-400-error-response.json";

    mockDefaultConfiguration(getWiremockUrl());

    EqualToJsonPattern postBodyPattern = new EqualToJsonPattern("{\n  \"contentType\" : 1,\n  \"packageName\" : \"TEST_NAME\",\n  \"customCoverage\" : {\n    \"beginCoverage\" : \"2017-12-23\",\n    \"endCoverage\" : \"2018-03-30\"\n  }\n}", false, true);

    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors.*"), providerStubResponseFile);
    mockPost(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/"+ STUB_VENDOR_ID + "/packages"),
      postBodyPattern, response, SC_BAD_REQUEST);

    RestAssured.given()
      .spec(getRequestSpecification())
      .body(readFile(packagePostStubRequestFile))
      .when()
      .post("eholdings/packages")
      .then()
      .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturnDefaultResourcesOnGetWithResources() throws IOException, URISyntaxException {

    String packageResourcesUrl = "/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources";
    String query = "?searchfield=titlename&selection=all&resourcetype=all&searchtype=advanced&search=&offset=1&count=25&orderby=titlename";
    shouldReturnResourcesOnGetWithResources(packageResourcesUrl, query);
  }

  @Test
  public void shouldReturnResourcesWithPagingOnGetWithResources() throws IOException, URISyntaxException {

    String packageResourcesUrl = "/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources?page=2";
    String query = "?searchfield=titlename&selection=all&resourcetype=all&searchtype=advanced&search=&offset=2&count=25&orderby=titlename";
    shouldReturnResourcesOnGetWithResources(packageResourcesUrl, query);
  }

  @Test
  public void shouldReturnResourcesWithTagsOnGetWithResources() throws IOException, URISyntaxException {
    try {
      TagsTestUtil.insertTag(vertx, "295-2545963-2099944", RecordType.RESOURCE, STUB_TAG_VALUE);
      TagsTestUtil.insertTag(vertx, "295-2545963-2172685", RecordType.RESOURCE, STUB_TAG_VALUE_2);
      TagsTestUtil.insertTag(vertx, "295-2545963-2172685", RecordType.RESOURCE, STUB_TAG_VALUE_3);

      String packageResourcesUrl = "/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources";
      String query = "?searchfield=titlename&selection=all&resourcetype=all&searchtype=advanced&search=&offset=1&count=25&orderby=titlename";

      mockDefaultConfiguration(getWiremockUrl());

      mockResourceById(RESOURCES_BY_PACKAGE_ID_STUB_FILE);

      String actual = getWithStatus(packageResourcesUrl, 200).asString();
      String expected = readFile(EXPECTED_RESOURCES_WITH_TAGS_STUB_FILE);

      JSONAssert.assertEquals(expected, actual, false);

      verify(1, getRequestedFor(urlEqualTo(RESOURCES_BY_PACKAGE_ID_URL + query)));
    } finally {
      TagsTestUtil.clearTags(vertx);
    }
  }

  @Test
  public void shouldReturnResourcesWithOnSearchByTags() throws IOException, URISyntaxException {
    try {
      mockDefaultConfiguration(getWiremockUrl());
      mockResourceById("responses/rmapi/resources/get-resource-by-id-success-response.json");

      ResourcesTestUtil.addResource(vertx, ResourcesTestUtil.DbResources.builder().id(STUB_MANAGED_RESOURCE_ID).name(
        STUB_TITLE_NAME).build());

      TagsTestUtil.insertTag(vertx, STUB_MANAGED_RESOURCE_ID, RecordType.RESOURCE, STUB_TAG_VALUE);

      String packageResourcesUrl = "/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID
        + "/resources?filter[tags]=" + STUB_TAG_VALUE;

      String actualResponse = RestAssured.given().spec(getRequestSpecification()).when().get(packageResourcesUrl).then()
        .statusCode(200).extract().asString();

      JSONAssert.assertEquals(readFile("responses/kb-ebsco/resources/expected-tagged-resources.json"), actualResponse,
        false);
    } finally {
      TagsTestUtil.clearTags(vertx);
      TestUtil.clearDataFromTable(vertx, RESOURCES_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturn404OnGetWithResourcesWhenPackageNotFound() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new RegexPattern(RESOURCES_BY_PACKAGE_ID_URL + ".*"), SC_NOT_FOUND);

    JsonapiError error = getWithStatus("/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources",
      SC_NOT_FOUND).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), is("Package not found"));
  }

  @Test
  public void shouldReturn400OnGetWithResourcesWhenCountOutOfRange() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    String packageResourcesUrl = "/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources?count=500";

    getWithStatus(packageResourcesUrl, SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturn400OnGetWithResourcesWhenRMAPI400() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/packages/get-package-resources-400-response.json";

    mockDefaultConfiguration(getWiremockUrl());

    stubFor(
      get(
        new UrlPathPattern(new RegexPattern(
          PACKAGE_BY_ID_URL + "/titles.*" ),
          true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))
          .withStatus(SC_BAD_REQUEST)));

    JsonapiError error = getWithStatus("/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources",
      SC_BAD_REQUEST).as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), is("Parameter Count is outside the range 1-100."));
  }


  @Test
  public void shouldReturnUnauthorizedOnGetWithResourcesWhenRMAPI401() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new RegexPattern(PACKAGE_BY_ID_URL + "/titles.*" ), HttpStatus.SC_UNAUTHORIZED);

    JsonapiError error = getWithStatus("/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources",
      HttpStatus.SC_FORBIDDEN).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized Access"));
  }

  @Test
  public void shouldReturnUnauthorizedOnGetWithResourcesWhenRMAPI403() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new RegexPattern(PACKAGE_BY_ID_URL + "/titles.*" ), HttpStatus.SC_FORBIDDEN);

    JsonapiError error = getWithStatus("/eholdings/packages/" + STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID + "/resources",
      HttpStatus.SC_FORBIDDEN).as(JsonapiError.class);
    assertThat(error.getErrors().get(0).getTitle(), containsString("Unauthorized Access"));
  }

  private void shouldReturnResourcesOnGetWithResources(String getURL, String rmAPIQuery) throws IOException, URISyntaxException {

    mockDefaultConfiguration(getWiremockUrl());

    mockResourceById(RESOURCES_BY_PACKAGE_ID_STUB_FILE);

    String actual = getWithStatus(getURL, 200).asString();
    String expected = readFile(EXPECTED_RESOURCES_STUB_FILE);

    JSONAssert.assertEquals(expected, actual, false);

    verify(1, getRequestedFor(urlEqualTo(RESOURCES_BY_PACKAGE_ID_URL + rmAPIQuery)));
  }

  private void mockResourceById(String stubFile) throws IOException, URISyntaxException {
    mockGet(new RegexPattern(RESOURCES_BY_PACKAGE_ID_URL + ".*" ), stubFile);
  }

  private void mockUpdateWithDeleteScenario(UrlPathPattern urlPattern, String initialPackage){
    mockUpdateScenario(urlPattern, initialPackage);

    stubFor(
      get(urlPattern)
        .inScenario(GET_PACKAGE_SCENARIO)
        .whenScenarioStateIs(PACKAGED_UPDATED_STATE)
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(SC_NOT_FOUND)));
  }

  private void mockUpdateScenario(UrlPathPattern urlPattern, String initialPackage, String updatedPackage){
    mockUpdateScenario(urlPattern, initialPackage);

    stubFor(
      get(urlPattern)
        .inScenario(GET_PACKAGE_SCENARIO)
        .whenScenarioStateIs(PACKAGED_UPDATED_STATE)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(updatedPackage)));
  }

  private void mockUpdateScenario(UrlPathPattern urlPattern, String initialPackage) {
    stubFor(
      get(urlPattern)
        .inScenario(GET_PACKAGE_SCENARIO)
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(initialPackage)));

    stubFor(
      put(urlPattern)
        .inScenario(GET_PACKAGE_SCENARIO)
        .willReturn(new ResponseDefinitionBuilder()
          .withStatus(SC_NO_CONTENT))
        .willSetStateTo(PACKAGED_UPDATED_STATE));
  }

  private void sendPutWithTags(List<String> newTags) throws IOException, URISyntaxException {
    sendPutWithTags(readFile(PACKAGE_STUB_FILE), newTags);
  }

  private void sendPutWithTags(String mockUpdatedPackage, List<String> newTags) throws IOException, URISyntaxException {
    ObjectMapper mapper = new ObjectMapper();

    mockDefaultConfiguration(getWiremockUrl());

    UrlPathPattern urlPattern = new UrlPathPattern(new EqualToPattern(PACKAGE_BY_ID_URL), false);
    mockUpdateScenario(urlPattern, readFile(PACKAGE_STUB_FILE), mockUpdatedPackage);

    PackagePutRequest packageToBeUpdated = mapper.readValue(getFile("requests/kb-ebsco/package/put-package-selected.json"), PackagePutRequest.class);

    if(newTags != null) {
      packageToBeUpdated.getData().getAttributes().setTags(new Tags()
        .withTagList(newTags));
    }

    putWithOk(PACKAGES_PATH, mapper.writeValueAsString(packageToBeUpdated)).as(Package.class);
  }

  private String sendPostWithTags(String requestBody, List<String> tags) throws IOException, URISyntaxException {
    String providerStubResponseFile = "responses/rmapi/packages/get-package-provider-by-id.json";
    String packageCreatedIdStubResponseFile = "responses/rmapi/packages/post-package-response.json";
    String packageByIdStubResponseFile = "responses/rmapi/packages/get-package-by-id-response.json";

    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/"), providerStubResponseFile);
    mockPost(new EqualToPattern(PACKAGES_STUB_URL), new AnythingPattern(), packageCreatedIdStubResponseFile, SC_OK);
    mockGet(new EqualToPattern(PACKAGE_BY_ID_URL), packageByIdStubResponseFile);

    ObjectMapper mapper = new ObjectMapper();
    PackagePostRequest request = mapper.readValue(requestBody, PackagePostRequest.class);
    request.getData().getAttributes()
      .withTags(new Tags().withTagList(tags));
    return RestAssured.given()
      .spec(getRequestSpecification())
      .header(CONTENT_TYPE_HEADER)
      .body(mapper.writeValueAsString(request))
      .when()
      .post("eholdings/packages")
      .then()
      .statusCode(SC_OK).extract().body().asString();
  }
}
