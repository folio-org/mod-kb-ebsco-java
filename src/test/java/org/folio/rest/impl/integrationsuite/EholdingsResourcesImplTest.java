package org.folio.rest.impl.integrationsuite;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.folio.repository.RecordType.RESOURCE;
import static org.folio.repository.accesstypes.AccessTypeMappingsTableConstants.ACCESS_TYPES_MAPPING_TABLE_NAME;
import static org.folio.repository.accesstypes.AccessTypesTableConstants.ACCESS_TYPES_TABLE_NAME;
import static org.folio.repository.resources.ResourceTableConstants.RESOURCES_TABLE_NAME;
import static org.folio.repository.tag.TagTableConstants.TAGS_TABLE_NAME;
import static org.folio.rest.impl.PackagesTestData.STUB_PACKAGE_ID;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_ID;
import static org.folio.rest.impl.ProvidersTestData.STUB_VENDOR_NAME;
import static org.folio.rest.impl.ResourcesTestData.STUB_CUSTOM_RESOURCE_ID;
import static org.folio.rest.impl.ResourcesTestData.STUB_MANAGED_RESOURCE_ID;
import static org.folio.rest.impl.TagsTestData.STUB_TAG_VALUE;
import static org.folio.rest.impl.TagsTestData.STUB_TAG_VALUE_2;
import static org.folio.rest.impl.TitlesTestData.STUB_CUSTOM_PACKAGE_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_CUSTOM_TITLE_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_CUSTOM_VENDOR_ID;
import static org.folio.rest.impl.TitlesTestData.STUB_MANAGED_TITLE_ID;
import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;
import static org.folio.rest.util.RestConstants.PROVIDERS_TYPE;
import static org.folio.rest.util.RestConstants.TITLES_TYPE;
import static org.folio.test.util.TestUtil.getFile;
import static org.folio.test.util.TestUtil.mockGet;
import static org.folio.test.util.TestUtil.mockPut;
import static org.folio.test.util.TestUtil.readFile;
import static org.folio.test.util.TestUtil.readJsonFile;
import static org.folio.util.AccessTypesTestUtil.getAccessTypeMappings;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypeMapping;
import static org.folio.util.AccessTypesTestUtil.insertAccessTypes;
import static org.folio.util.AccessTypesTestUtil.testData;
import static org.folio.util.KBTestUtil.clearDataFromTable;
import static org.folio.util.KBTestUtil.mockDefaultConfiguration;
import static org.folio.util.TagsTestUtil.insertTag;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypeMapping;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.Resource;
import org.folio.rest.jaxrs.model.ResourceBulkFetchCollection;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.folio.rest.jaxrs.model.ResourceTags;
import org.folio.rest.jaxrs.model.ResourceTagsPutRequest;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.util.ResourcesTestUtil;
import org.folio.util.TagsTestUtil;

@RunWith(VertxUnitRunner.class)
public class EholdingsResourcesImplTest extends WireMockTestBase {

  private static final String MANAGED_PACKAGE_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID + "/packages/" + STUB_PACKAGE_ID;
  private static final String MANAGED_RESOURCE_ENDPOINT = MANAGED_PACKAGE_ENDPOINT + "/titles/" + STUB_MANAGED_TITLE_ID;
  private static final String CUSTOM_RESOURCE_ENDPOINT = "/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_CUSTOM_VENDOR_ID + "/packages/" + STUB_CUSTOM_PACKAGE_ID + "/titles/" + STUB_CUSTOM_TITLE_ID;
  private static final String STUB_MANAGED_RESOURCE_PATH = "eholdings/resources/" + STUB_MANAGED_RESOURCE_ID;
  private static final String RESOURCE_TAGS_PATH = "eholdings/resources/" + STUB_CUSTOM_RESOURCE_ID + "/tags";
  private static final String RESOURCES_BULK_FETCH = "/eholdings/resources/bulk/fetch";

  @Test
  public void shouldReturnResourceWhenValidId() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";

    mockDefaultConfiguration(getWiremockUrl());
    mockResource(stubResponseFile, MANAGED_RESOURCE_ENDPOINT);

    String actualResponse = getWithOk(STUB_MANAGED_RESOURCE_PATH).asString();

    JSONAssert.assertEquals(
      readFile("responses/kb-ebsco/resources/expected-resource-by-id.json"), actualResponse, false);
  }

  @Test
  public void shouldReturnResourceWithTags() throws IOException, URISyntaxException {
    try {
      insertTag(vertx, STUB_MANAGED_RESOURCE_ID, RecordType.RESOURCE, STUB_TAG_VALUE);
      String stubResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";

      mockDefaultConfiguration(getWiremockUrl());
      mockResource(stubResponseFile, MANAGED_RESOURCE_ENDPOINT);

      Resource resource = getWithOk(STUB_MANAGED_RESOURCE_PATH).as(Resource.class);

      assertTrue(resource.getData().getAttributes().getTags().getTagList().contains(STUB_TAG_VALUE));
    } finally {
      clearDataFromTable(vertx, TAGS_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturnResourceWithTitleWhenTitleFlagSetToTrue() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";
    String expectedResourceFile = "responses/kb-ebsco/resources/expected-resource-by-id.json";
    String expectedTitleForResourceFile = "responses/kb-ebsco/titles/expected-title-by-id-for-resource.json";

    mockDefaultConfiguration(getWiremockUrl());

    mockResource(stubResponseFile, MANAGED_RESOURCE_ENDPOINT);

    String actualResponse = getWithOk("eholdings/resources/" + STUB_MANAGED_RESOURCE_ID + "?include=title")
      .asString();

    ObjectMapper mapper = new ObjectMapper();
    Resource resource = mapper.readValue(readFile(expectedResourceFile), Resource.class);
    Object includedItem = mapper.readValue(readFile(expectedTitleForResourceFile), Object.class);
    resource.withIncluded(
      Collections.singletonList(includedItem));
    resource.getData()
      .getRelationships()
      .withTitle(new HasOneRelationship()
        .withData(new RelationshipData()
          .withType(TITLES_TYPE)
          .withId(STUB_MANAGED_TITLE_ID)));
    JSONAssert.assertEquals(
      mapper.writeValueAsString(resource), actualResponse, false);
  }

  @Test
  public void shouldReturnResourceWithProviderWhenProviderFlagSetToTrue() throws IOException, URISyntaxException {
    String stubResourceResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";
    String stubVendorResponseFile = "responses/rmapi/vendors/get-vendor-by-id-for-resource.json";
    String expectedResourceFile = "responses/kb-ebsco/resources/expected-resource-by-id.json";
    String expectedProviderForResourceFile = "responses/kb-ebsco/providers/expected-provider-by-id-for-resource.json";

    mockDefaultConfiguration(getWiremockUrl());

    mockResource(stubResourceResponseFile, MANAGED_RESOURCE_ENDPOINT);
    mockVendor(stubVendorResponseFile);

    String actualResponse = getWithOk("eholdings/resources/" + STUB_MANAGED_RESOURCE_ID + "?include=provider").asString();

    ObjectMapper mapper = new ObjectMapper();

    Resource resource = mapper.readValue(readFile(expectedResourceFile), Resource.class);
    Object includedItem = mapper.readValue(readFile(expectedProviderForResourceFile), Object.class);
    resource.withIncluded(
      Collections.singletonList(includedItem));
    resource.getData()
      .getRelationships()
      .withProvider(new HasOneRelationship()
        .withData(new RelationshipData()
          .withType(PROVIDERS_TYPE)
          .withId(STUB_VENDOR_ID)));
    JSONAssert.assertEquals(
      mapper.writeValueAsString(resource), actualResponse, false);
  }

  @Test
  public void shouldReturnResourceWithPackageWhenPackageFlagSetToTrue() throws IOException, URISyntaxException {
    String stubResourceResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";
    String stubPackageResponseFile = "responses/rmapi/packages/get-package-by-id-for-resource.json";
    String expectedResourceFile = "responses/kb-ebsco/resources/expected-resource-by-id.json";
    String expectedPackageForResourceFile = "responses/kb-ebsco/packages/expected-package-by-id-for-resource.json";

    mockDefaultConfiguration(getWiremockUrl());

    mockResource(stubResourceResponseFile, MANAGED_RESOURCE_ENDPOINT);
    mockPackage(stubPackageResponseFile);

    String actualResponse = getWithOk("eholdings/resources/" + STUB_MANAGED_RESOURCE_ID + "?include=package")
      .asString();

    ObjectMapper mapper = new ObjectMapper();

    Resource resource = mapper.readValue(readFile(expectedResourceFile), Resource.class);
    Object includedItem = mapper.readValue(readFile(expectedPackageForResourceFile), Object.class);
    resource.withIncluded(
      Collections.singletonList(includedItem));
    resource.getData()
      .getRelationships()
      .withPackage(new HasOneRelationship()
        .withData(new RelationshipData()
          .withType(PACKAGES_TYPE)
          .withId(STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID)));
    JSONAssert.assertEquals(
      mapper.writeValueAsString(resource), actualResponse, false);
  }

  @Test
  public void shouldReturnResourceWithAllIncludedObjectsWhenIncludeContainsAllObjects()
    throws IOException, URISyntaxException {
    String stubResourceResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";
    String stubVendorResponseFile = "responses/rmapi/vendors/get-vendor-by-id-for-resource.json";
    String stubPackageResponseFile = "responses/rmapi/packages/get-package-by-id-for-resource.json";
    String expectedResourceFile = "responses/kb-ebsco/resources/expected-resource-by-id.json";
    String expectedProviderForResourceFile = "responses/kb-ebsco/providers/expected-provider-by-id-for-resource.json";
    String expectedPackageForResourceFile = "responses/kb-ebsco/packages/expected-package-by-id-for-resource.json";
    String expectedTitleForResourceFile = "responses/kb-ebsco/titles/expected-title-by-id-for-resource.json";

    mockDefaultConfiguration(getWiremockUrl());

    mockResource(stubResourceResponseFile, MANAGED_RESOURCE_ENDPOINT);
    mockVendor(stubVendorResponseFile);
    mockPackage(stubPackageResponseFile);

    String actualResponse = getWithOk(
      "eholdings/resources/" + STUB_MANAGED_RESOURCE_ID + "?include=package,title,provider").asString();

    ObjectMapper mapper = new ObjectMapper();

    Resource resource = mapper.readValue(readFile(expectedResourceFile), Resource.class);
    Object includedProvider = mapper.readValue(readFile(expectedProviderForResourceFile), Object.class);
    Object includedTitle = mapper.readValue(readFile(expectedTitleForResourceFile), Object.class);
    Object includedPackage = mapper.readValue(readFile(expectedPackageForResourceFile), Object.class);

    resource.withIncluded(
      Arrays.asList(includedProvider, includedTitle, includedPackage));
    resource.getData()
      .getRelationships()
      .withPackage(new HasOneRelationship()
        .withData(new RelationshipData()
          .withType(PACKAGES_TYPE)
          .withId(STUB_VENDOR_ID + "-" + STUB_PACKAGE_ID)))
      .withProvider(new HasOneRelationship()
        .withData(new RelationshipData()
          .withType(PROVIDERS_TYPE)
          .withId(STUB_VENDOR_ID)))
      .withTitle(new HasOneRelationship()
        .withData(new RelationshipData()
          .withType(TITLES_TYPE)
          .withId(STUB_MANAGED_TITLE_ID)));
    JSONAssert.assertEquals(
      mapper.writeValueAsString(resource), actualResponse, false);
  }

  @Test
  public void shouldReturn404WhenRMAPINotFoundOnResourceGet() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-resource-by-id-not-found-response.json";

    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new RegexPattern(MANAGED_PACKAGE_ENDPOINT + "/titles.*"), true))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(stubResponseFile))
          .withStatus(404)));

    JsonapiError error = getWithStatus(STUB_MANAGED_RESOURCE_PATH, SC_NOT_FOUND)
      .as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Resource not found"));
  }

  @Test
  public void shouldReturn400WhenValidationErrorOnResourceGet() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());


    JsonapiError error = getWithStatus("eholdings/resources/583-abc-762169", SC_BAD_REQUEST)
      .as(JsonapiError.class);

    assertThat(error.getErrors().get(0).getTitle(), equalTo("Resource id is invalid - 583-abc-762169"));
  }

  @Test
  public void shouldReturn500WhenRMApiReturns500ErrorOnResourceGet() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new RegexPattern(MANAGED_PACKAGE_ENDPOINT + "/titles.*"), SC_INTERNAL_SERVER_ERROR);

    getWithStatus(STUB_MANAGED_RESOURCE_PATH, SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void shouldReturnUpdatedValuesManagedResourceOnSuccessfulPut() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-managed-resource-updated-response.json";
    String expectedResourceFile = "responses/kb-ebsco/resources/expected-managed-resource.json";

    String actualResponse = mockUpdateResourceScenario(stubResponseFile, MANAGED_RESOURCE_ENDPOINT, STUB_MANAGED_RESOURCE_ID,
      readFile("requests/kb-ebsco/resource/put-managed-resource.json"));

    JSONAssert.assertEquals(readFile(expectedResourceFile), actualResponse, false);

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern(MANAGED_RESOURCE_ENDPOINT), true))
      .withRequestBody(
        equalToJson(readFile("requests/rmapi/resources/put-managed-resource-is-selected-multiple-attributes.json"))));
  }

  @Test
  public void shouldCreateNewAccessTypeMappingOnSuccessfulPut() throws IOException, URISyntaxException {
    try {
      List<AccessTypeCollectionItem> accessTypes = insertAccessTypes(testData(), vertx);
      String accessTypeId = accessTypes.get(0).getId();
      String stubResponseFile = "responses/rmapi/resources/get-managed-resource-updated-response.json";
      String expectedResourceFile = "responses/kb-ebsco/resources/expected-managed-resource-with-access-type.json";

      String requestBody = format(readFile("requests/kb-ebsco/resource/put-resource-with-access-type.json"),
        accessTypeId);
      String actualResponse = mockUpdateResourceScenario(stubResponseFile, MANAGED_RESOURCE_ENDPOINT,
        STUB_MANAGED_RESOURCE_ID, requestBody);

      String expectedJson = format(readFile(expectedResourceFile), accessTypeId, accessTypeId);
      JSONAssert.assertEquals(expectedJson, actualResponse, false);

      verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern(MANAGED_RESOURCE_ENDPOINT), true))
        .withRequestBody(
          equalToJson(readFile("requests/rmapi/resources/put-managed-resource-is-selected-multiple-attributes.json"))));

      List<AccessTypeMapping> accessTypeMappingsInDB = getAccessTypeMappings(vertx);
      assertEquals(1, accessTypeMappingsInDB.size());
      assertEquals(accessTypeId, accessTypeMappingsInDB.get(0).getAccessTypeId());
      assertEquals(RESOURCE, accessTypeMappingsInDB.get(0).getRecordType());
    } finally {
      clearDataFromTable(vertx, ACCESS_TYPES_TABLE_NAME);
      clearDataFromTable(vertx, ACCESS_TYPES_MAPPING_TABLE_NAME);
    }
  }

  @Test
  public void shouldDeleteAccessTypeMappingOnSuccessfulPut() throws IOException, URISyntaxException {
    try {
      List<AccessTypeCollectionItem> accessTypes = insertAccessTypes(testData(), vertx);
      String accessTypeId = accessTypes.get(0).getId();
      insertAccessTypeMapping(STUB_MANAGED_RESOURCE_ID, RESOURCE, accessTypeId, vertx);

      String stubResponseFile = "responses/rmapi/resources/get-managed-resource-updated-response.json";
      String expectedResourceFile = "responses/kb-ebsco/resources/expected-managed-resource.json";

      String actualResponse =
        mockUpdateResourceScenario(stubResponseFile, MANAGED_RESOURCE_ENDPOINT, STUB_MANAGED_RESOURCE_ID,
          readFile("requests/kb-ebsco/resource/put-managed-resource.json"));

      JSONAssert.assertEquals(readFile(expectedResourceFile), actualResponse, false);

      verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern(MANAGED_RESOURCE_ENDPOINT), true))
        .withRequestBody(
          equalToJson(readFile("requests/rmapi/resources/put-managed-resource-is-selected-multiple-attributes.json"))));

      List<AccessTypeMapping> accessTypeMappingsInDB = getAccessTypeMappings(vertx);
      assertEquals(0, accessTypeMappingsInDB.size());
    } finally {
      clearDataFromTable(vertx, ACCESS_TYPES_TABLE_NAME);
      clearDataFromTable(vertx, ACCESS_TYPES_MAPPING_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturn400OnPutPackageWithNotExistedAccessType() throws URISyntaxException, IOException {
    mockDefaultConfiguration(getWiremockUrl());

    String requestBody = readFile("requests/kb-ebsco/resource/put-managed-resource-with-missing-access-type.json");

    JsonapiError error = putWithStatus(STUB_MANAGED_RESOURCE_PATH, requestBody, SC_BAD_REQUEST, CONTENT_TYPE_HEADER)
      .as(JsonapiError.class);

    verify(0, putRequestedFor(new UrlPathPattern(new RegexPattern(MANAGED_RESOURCE_ENDPOINT), true)));

    assertEquals(1, error.getErrors().size());
    assertEquals("Access type not found by id: 99999999-9999-1999-a999-999999999999", error.getErrors().get(0).getTitle());
  }

  @Test
  public void shouldReturn422OnPutPackageWithInvalidAccessTypeId() throws URISyntaxException, IOException {
    mockDefaultConfiguration(getWiremockUrl());

    String requestBody = readFile("requests/kb-ebsco/resource/put-managed-resource-with-invalid-access-type.json");

    Errors error = putWithStatus(STUB_MANAGED_RESOURCE_PATH, requestBody, SC_UNPROCESSABLE_ENTITY, CONTENT_TYPE_HEADER)
      .as(Errors.class);

    verify(0, putRequestedFor(new UrlPathPattern(new RegexPattern(MANAGED_RESOURCE_ENDPOINT), true)));

    assertEquals(1, error.getErrors().size());
    assertEquals("must match \"^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$\"",
      error.getErrors().get(0).getMessage());
  }

  @Test
  public void shouldDeselectManagedResourceOnPutWithSelectedFalse() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-managed-resource-updated-response-is-selected-false.json";
    String expectedResourceFile = "responses/kb-ebsco/resources/expected-managed-resource.json";

    ResourcePutRequest request =
      readJsonFile("requests/kb-ebsco/resource/put-managed-resource-is-not-selected.json", ResourcePutRequest.class);
    request.getData().getAttributes().setIsSelected(false);
    String actualResponse = mockUpdateResourceScenario(stubResponseFile, MANAGED_RESOURCE_ENDPOINT, STUB_MANAGED_RESOURCE_ID,
      Json.encode(request));

    Resource expectedResource = readJsonFile(expectedResourceFile, Resource.class);
    expectedResource.getData().getAttributes().setIsSelected(false);
    JSONAssert.assertEquals(Json.encode(expectedResource), actualResponse, false);

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern(MANAGED_RESOURCE_ENDPOINT), true))
      .withRequestBody(new EqualToJsonPattern(readFile("requests/rmapi/resources/put-managed-resource-is-not-selected.json"),
        true, true)));
  }

  @Test
  public void shouldReturnUpdatedValuesCustomResourceOnSuccessfulPut() throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-custom-resource-updated-response.json";
    String expectedResourceFile = "responses/kb-ebsco/resources/expected-custom-resource.json";

    String actualResponse = mockUpdateResourceScenario(stubResponseFile, CUSTOM_RESOURCE_ENDPOINT, STUB_CUSTOM_RESOURCE_ID,
      readFile("requests/kb-ebsco/resource/put-custom-resource.json"));

    JSONAssert.assertEquals(readFile(expectedResourceFile), actualResponse, false);

    verify(1, putRequestedFor(new UrlPathPattern(new RegexPattern(CUSTOM_RESOURCE_ENDPOINT), true))
      .withRequestBody(
        equalToJson(readFile("requests/rmapi/resources/put-custom-resource-is-selected-multiple-attributes.json"))));
  }

  @Test
  public void shouldUpdateTagsOnSuccessfulTagsPut() throws IOException, URISyntaxException {
    try {
      List<String> tags = Collections.singletonList(STUB_TAG_VALUE);
      sendPutTags(tags, RESOURCE_TAGS_PATH);
      List<ResourcesTestUtil.DbResources> resources = ResourcesTestUtil.getResources(vertx);
      assertEquals(1, resources.size());
      assertEquals(STUB_CUSTOM_RESOURCE_ID, resources.get(0).getId());
      assertEquals(STUB_VENDOR_NAME, resources.get(0).getName());
    } finally {
      clearDataFromTable(vertx, TAGS_TABLE_NAME);
      clearDataFromTable(vertx, RESOURCES_TABLE_NAME);
    }
  }

  @Test
  public void shouldUpdateTagsOnSuccessfulTagsPutWithAlreadyExistingTags() throws IOException, URISyntaxException {
    try {
      insertTag(vertx, STUB_CUSTOM_RESOURCE_ID, RecordType.RESOURCE, STUB_TAG_VALUE);
      List<String> newTags = Arrays.asList(STUB_TAG_VALUE, STUB_TAG_VALUE_2);
      sendPutTags(newTags, RESOURCE_TAGS_PATH);
      List<String> tagsAfterRequest = TagsTestUtil.getTagsForRecordType(vertx, RecordType.RESOURCE);
      assertThat(tagsAfterRequest, containsInAnyOrder(newTags.toArray()));
    } finally {
      clearDataFromTable(vertx, TAGS_TABLE_NAME);
      clearDataFromTable(vertx, RESOURCES_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturn422OnPutTagsWhenRequestBodyIsInvalid() throws IOException, URISyntaxException {
    ObjectMapper mapper = new ObjectMapper();
    ResourceTagsPutRequest tags =
      mapper.readValue(getFile("requests/kb-ebsco/resource/put-resource-tags.json"), ResourceTagsPutRequest.class);
    tags.getData().getAttributes().setName("");
    putWithStatus(RESOURCE_TAGS_PATH, mapper.writeValueAsString(tags), SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void shouldReturn422WhenInvalidUrlIsProvidedForCustomResource() throws URISyntaxException, IOException {
    final String stubResponseFile = "responses/rmapi/resources/get-custom-resource-updated-response.json";
    final String invalidPutBody = readFile("requests/kb-ebsco/resource/put-custom-resource-invalid-url.json");

    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new RegexPattern(CUSTOM_RESOURCE_ENDPOINT), stubResponseFile);

    putWithStatus("eholdings/resources/" + STUB_CUSTOM_RESOURCE_ID, invalidPutBody, SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void shouldPostResourceToRMAPI() throws IOException, URISyntaxException {
    String stubTitleResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";
    String stubPackageResponseFile = "responses/rmapi/packages/get-custom-package-by-id-response.json";
    String stubPackageResourcesFile = "responses/rmapi/resources/get-resources-by-package-id-response.json";
    String postStubRequest = "requests/kb-ebsco/resource/post-resources-request.json";

    String expectedResourceFile = "responses/kb-ebsco/resources/expected-resource-by-id.json";

    EqualToJsonPattern putRequestBodyPattern =
      new EqualToJsonPattern(readFile("requests/rmapi/resources/select-resource-request.json"),
        true, true);

    mockDefaultConfiguration(getWiremockUrl());

    mockPackageResources(stubPackageResourcesFile);
    mockPackage(stubPackageResponseFile);
    mockTitle(stubTitleResponseFile);
    mockResource(stubTitleResponseFile, MANAGED_RESOURCE_ENDPOINT);

    mockPut(new RegexPattern(MANAGED_RESOURCE_ENDPOINT), putRequestBodyPattern, SC_NO_CONTENT);

    String actualResponse = postWithOk("eholdings/resources", readFile(postStubRequest)).asString();

    JSONAssert.assertEquals(
      readFile(expectedResourceFile), actualResponse, false);

    verify(1, putRequestedFor(new UrlPathPattern(new EqualToPattern(MANAGED_RESOURCE_ENDPOINT), false))
      .withRequestBody(putRequestBodyPattern));
  }

  @Test
  public void shouldReturn404IfTitleOrPackageIsNotFound() throws IOException, URISyntaxException {
    String postStubRequest = "requests/kb-ebsco/resource/post-resources-request.json";

    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), SC_NOT_FOUND);
    mockGet(new RegexPattern(MANAGED_PACKAGE_ENDPOINT), SC_NOT_FOUND);

    postWithStatus("eholdings/resources", readFile(postStubRequest), SC_NOT_FOUND);
  }

  @Test
  public void shouldReturn422IfPackageIsNotCustom() throws IOException, URISyntaxException {
    String stubTitleResponseFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";
    String stubPackageResponseFile = "responses/rmapi/packages/get-package-by-id-for-resource.json";
    String stubPackageResourcesFile = "responses/rmapi/resources/get-resources-by-package-id-response.json";
    String postStubRequest = "requests/kb-ebsco/resource/post-resources-request.json";

    mockDefaultConfiguration(getWiremockUrl());

    mockPackageResources(stubPackageResourcesFile);
    mockPackage(stubPackageResponseFile);
    mockTitle(stubTitleResponseFile);

    postWithStatus("eholdings/resources", readFile(postStubRequest), SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void shouldSendDeleteRequestForResourceAssociatedWithCustomPackage() throws IOException, URISyntaxException {
    EqualToJsonPattern putBodyPattern = new EqualToJsonPattern("{\"isSelected\":false}", true, true);
    deleteResource(putBodyPattern, CUSTOM_RESOURCE_ENDPOINT);
    verify(1, putRequestedFor(new UrlPathPattern(new EqualToPattern(CUSTOM_RESOURCE_ENDPOINT), false))
      .withRequestBody(putBodyPattern));
  }

  @Test
  public void shouldDeleteTagsOnDeleteRequest() throws IOException, URISyntaxException {
    try {
      insertTag(vertx, STUB_CUSTOM_RESOURCE_ID, RecordType.RESOURCE, STUB_TAG_VALUE);
      EqualToJsonPattern putBodyPattern = new EqualToJsonPattern("{\"isSelected\":false}", true, true);
      deleteResource(putBodyPattern, CUSTOM_RESOURCE_ENDPOINT);
      List<String> actualTags = TagsTestUtil.getTags(vertx);
      assertThat(actualTags, empty());
    } finally {
      clearDataFromTable(vertx, TAGS_TABLE_NAME);
    }
  }

  @Test
  public void shouldDeleteAccessTypeOnDeleteRequest() throws IOException, URISyntaxException {
    try {
      String accessTypeId = insertAccessTypes(testData(), vertx).get(0).getId();
      insertAccessTypeMapping(STUB_CUSTOM_RESOURCE_ID, RecordType.RESOURCE, accessTypeId, vertx);
      EqualToJsonPattern putBodyPattern = new EqualToJsonPattern("{\"isSelected\":false}", true, true);
      deleteResource(putBodyPattern, CUSTOM_RESOURCE_ENDPOINT);
      List<AccessTypeMapping> actualMappings = getAccessTypeMappings(vertx);
      assertThat(actualMappings, empty());
    } finally {
      clearDataFromTable(vertx, ACCESS_TYPES_TABLE_NAME);
      clearDataFromTable(vertx, ACCESS_TYPES_MAPPING_TABLE_NAME);
    }
  }

  @Test
  public void shouldReturn400WhenResourceIdIsInvalid() {
    deleteWithStatus("eholdings/resources/abc-def", SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturn400WhenTryingToDeleteResourceAssociatedWithManagedPackage()
    throws URISyntaxException, IOException {
    String stubResponseFile = "responses/rmapi/resources/get-managed-resource-updated-response.json";
    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new EqualToPattern(MANAGED_RESOURCE_ENDPOINT), stubResponseFile);
    deleteWithStatus(STUB_MANAGED_RESOURCE_PATH, SC_BAD_REQUEST);
  }

  @Test
  public void shouldReturnListWithBulkFetchResources() throws IOException, URISyntaxException {
    String postBody = readFile("requests/kb-ebsco/resource/post-resources-bulk.json");
    String expectedResourceFile = "responses/kb-ebsco/resources/expected-resources-bulk-response.json";
    String responseRmApiFile = "responses/rmapi/resources/get-resource-by-id-success-response.json";

    mockGet(new EqualToPattern(MANAGED_RESOURCE_ENDPOINT), responseRmApiFile);
    mockDefaultConfiguration(getWiremockUrl());
    final String actualResponse = postWithOk("/eholdings/resources/bulk/fetch", postBody).asString();
    JSONAssert.assertEquals(
      readFile(expectedResourceFile), actualResponse, true);
  }

  @Test
  public void shouldReturnResourcesAndFailedIds() throws IOException, URISyntaxException {
    String postBody = readFile("requests/kb-ebsco/resource/post-resources-bulk-with-invalid-ids.json");
    String expectedResourceFile = "responses/kb-ebsco/resources/expected-resources-bulk-response-with-failed-ids.json";

    mockDefaultConfiguration(getWiremockUrl());

    String resourceResponse1 = "responses/rmapi/resources/get-resource-by-id-success-response.json";
    mockGet(new EqualToPattern(MANAGED_RESOURCE_ENDPOINT), resourceResponse1);

    String resourceResponse2 = "responses/rmapi/resources/get-custom-resource-updated-response.json";
    mockGet(new EqualToPattern(CUSTOM_RESOURCE_ENDPOINT), resourceResponse2);

    String notFoundResponse = "responses/rmapi/resources/get-resource-by-id-not-found-response.json";
    mockDefaultConfiguration(getWiremockUrl());
    stubFor(
      get(new UrlPathPattern(new EqualToPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/186/packages/3150130/titles/19087948"), false))
        .willReturn(new ResponseDefinitionBuilder()
          .withBody(readFile(notFoundResponse))
          .withStatus(404)));

    final String actualResponse = postWithOk(RESOURCES_BULK_FETCH, postBody).asString();
    JSONAssert.assertEquals(
      readFile(expectedResourceFile), actualResponse, false);
  }

  @Test
  public void shouldReturnErrorWhenRMApiFails() throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());
    mockGet(new RegexPattern(MANAGED_PACKAGE_ENDPOINT + "/titles.*"), SC_INTERNAL_SERVER_ERROR);

    String postBody = readFile("requests/kb-ebsco/resource/post-resources-bulk.json");
    final ResourceBulkFetchCollection bulkFetchCollection = postWithOk(RESOURCES_BULK_FETCH, postBody).as(ResourceBulkFetchCollection.class);

    assertThat(bulkFetchCollection.getIncluded().size(), equalTo(0));
    assertThat(bulkFetchCollection.getMeta().getFailed().getResources().size(), equalTo(1));
    assertThat(bulkFetchCollection.getMeta().getFailed().getResources().get(0), equalTo("19-3964-762169"));
  }

  private void mockPackageResources(String stubPackageResourcesFile) throws IOException, URISyntaxException {
    mockGet(new RegexPattern(MANAGED_PACKAGE_ENDPOINT + "/titles.*"), stubPackageResourcesFile);
  }

  private void mockPackage(String responseFile) throws IOException, URISyntaxException {
    mockGet(new RegexPattern(MANAGED_PACKAGE_ENDPOINT), responseFile);
  }

  private void mockResource(String responseFile, String resourceEndpoint) throws IOException, URISyntaxException {
    mockGet(new RegexPattern(resourceEndpoint), responseFile);
  }

  private void mockTitle(String responseFile) throws IOException, URISyntaxException {
    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/titles.*"), responseFile);
  }

  private void mockVendor(String responseFile) throws IOException, URISyntaxException {
    mockGet(new RegexPattern("/rm/rmaccounts/" + STUB_CUSTOMER_ID + "/vendors/" + STUB_VENDOR_ID), responseFile);
  }

  private String mockUpdateResourceScenario(String updatedResourceResponseFile, String resourceEndpoint, String resourceId,
                                            String requestBody) throws IOException, URISyntaxException {
    mockDefaultConfiguration(getWiremockUrl());

    stubFor(
      get(new UrlPathPattern(new RegexPattern(resourceEndpoint), false))
        .willReturn(new ResponseDefinitionBuilder().withBody(readFile(updatedResourceResponseFile))));

    stubFor(
      put(new UrlPathPattern(new RegexPattern(resourceEndpoint), true))
        .willReturn(new ResponseDefinitionBuilder().withStatus(SC_NO_CONTENT)));

    return putWithOk("eholdings/resources/" + resourceId, requestBody).asString();

  }

  private void sendPutTags(List<String> newTags, String url) throws IOException, URISyntaxException {
    ObjectMapper mapper = new ObjectMapper();

    ResourceTagsPutRequest tags =
      mapper.readValue(getFile("requests/kb-ebsco/resource/put-resource-tags.json"), ResourceTagsPutRequest.class);

    if (newTags != null) {
      tags.getData().getAttributes().setTags(new Tags()
        .withTagList(newTags));
    }

    putWithOk(url, mapper.writeValueAsString(tags)).as(ResourceTags.class);
  }

  private void deleteResource(EqualToJsonPattern putBodyPattern, String resourcePath)
    throws IOException, URISyntaxException {
    String stubResponseFile = "responses/rmapi/resources/get-custom-resource-updated-response.json";
    mockDefaultConfiguration(getWiremockUrl());

    mockGet(new EqualToPattern(resourcePath), stubResponseFile);
    mockPut(new EqualToPattern(resourcePath), putBodyPattern, SC_NO_CONTENT);

    deleteWithNoContent("eholdings/resources/" + STUB_CUSTOM_RESOURCE_ID);
  }
}
