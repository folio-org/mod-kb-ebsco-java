package org.folio.rest.converter.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.ResourcePut;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.UserDefinedFields;
import org.folio.holdingsiq.model.VisibilityInfo;
import org.folio.rest.impl.ResourcesTestData;
import org.folio.rest.jaxrs.model.EmbargoPeriod;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.ResourcePutDataAttributes;
import org.folio.rest.jaxrs.model.VisibilityData;

public class ResourceRequestConverterTest {
  private static final String OLD_PROXY_ID = "<n>";
  public static final String OLD_COVERAGE_STATEMENT = "statement";
  public static final String OLD_URL = "http://example.com";
  public static final boolean OLD_VISIBILITY_DATA = true;
  public static final String OLD_BEGIN_COVERAGE = "2002-10-10";
  public static final String OLD_END_COVERAGE = "2003-10-10";
  public static final String OLD_EMBARGO_UNIT = "Day";
  public static final int OLD_EMBARGO_VALUE = 5;
  private ResourceRequestConverter resourcesConverter = new ResourceRequestConverter();
  private Title resourceData;

  @Before
  public void setUp() {
     resourceData = Title.builder()
      .contributorsList(Collections.emptyList())
      .customerResourcesList(Collections.singletonList(CustomerResources.builder()
        .coverageStatement(OLD_COVERAGE_STATEMENT)
        .isSelected(false)
        .visibilityData(VisibilityInfo.builder()
          .isHidden(OLD_VISIBILITY_DATA).build())
        .customCoverageList(Collections.singletonList(CoverageDates.builder()
          .beginCoverage(OLD_BEGIN_COVERAGE).endCoverage(OLD_END_COVERAGE).build()))
        .customEmbargoPeriod(org.folio.holdingsiq.model.EmbargoPeriod.builder()
          .embargoUnit(OLD_EMBARGO_UNIT).embargoValue(OLD_EMBARGO_VALUE).build())
        .proxy(org.folio.holdingsiq.model.Proxy.builder()
          .id(OLD_PROXY_ID).inherited(true).build())
        .url(OLD_URL)
        .userDefinedFields(UserDefinedFields.builder().build())
        .build()
      ))
      .identifiersList(Collections.emptyList())
      .subjectsList(Collections.emptyList())
      .titleId(1)
      .build();
  }

  @Test
  public void shouldCreateRequestToSelectManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)), resourceData);
    assertTrue(resourcePut.getIsSelected());
  }

  @Test
  public void shouldCreateRequestToSelectCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)), resourceData);
    assertTrue(resourcePut.getIsSelected());
  }

  @Test
  public void shouldCreateRequestToUpdateProxyForManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withProxy(new Proxy()
          .withId("test-proxy-id"))), resourceData);
    assertEquals("test-proxy-id", resourcePut.getProxy().getId());
  }

  @Test
  public void shouldCreateRequestToUpdateProxyForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withProxy(new Proxy()
          .withId("test-proxy-id"))), resourceData);
    assertEquals("test-proxy-id", resourcePut.getProxy().getId());
  }

  @Test
  public void shouldCreateRequestToUpdateUserDefineFieldsForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withUserDefinedField1("test 1")
        .withUserDefinedField2("test 2")
        .withUserDefinedField3("test 3")
        .withUserDefinedField4("test 4")
        .withUserDefinedField5("test 5")), resourceData);
    assertEquals("test 1", resourcePut.getUserDefinedFields().getUserDefinedField1());
    assertEquals("test 2", resourcePut.getUserDefinedFields().getUserDefinedField2());
    assertEquals("test 3", resourcePut.getUserDefinedFields().getUserDefinedField3());
    assertEquals("test 4", resourcePut.getUserDefinedFields().getUserDefinedField4());
    assertEquals("test 5", resourcePut.getUserDefinedFields().getUserDefinedField5());
  }

  @Test
  public void shouldCreateRequestToUpdateIsHiddenForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withVisibilityData(new VisibilityData()
          .withIsHidden(true))), resourceData);
    assertTrue(resourcePut.getIsHidden());
  }

  @Test
  public void shouldCreateRequestToUpdateCoverageStatementForManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withCoverageStatement("test coverage stmt")), resourceData);
      assertEquals("test coverage stmt", resourcePut.getCoverageStatement());
  }

  @Test
  public void shouldCreateRequestToUpdateCoverageStatementForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withCoverageStatement("test coverage stmt")), resourceData);
    assertEquals("test coverage stmt", resourcePut.getCoverageStatement());
  }

  @Test
  public void shouldCreateRequestToUpdateIsHiddenForManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withVisibilityData(new VisibilityData()
            .withIsHidden(false))), resourceData);
      assertFalse(resourcePut.getIsHidden());
  }

  @Test
  public void shouldCreateRequestToUpdateCustomEmbargoPeriodForManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withCustomEmbargoPeriod(new EmbargoPeriod()
            .withEmbargoUnit(EmbargoUnit.DAYS)
            .withEmbargoValue(10))), resourceData);
      assertEquals("Days", resourcePut.getCustomEmbargoPeriod().getEmbargoUnit());
      assertEquals(10, resourcePut.getCustomEmbargoPeriod().getEmbargoValue());
  }

  @Test
  public void shouldCreateRequestToUpdateUserDefinedFieldsForManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withUserDefinedField1("test 1")
        .withUserDefinedField2("test 2")
        .withUserDefinedField3("test 3")
        .withUserDefinedField4("test 4")
        .withUserDefinedField5("test 5")), resourceData);
    assertEquals("test 1", resourcePut.getUserDefinedFields().getUserDefinedField1());
    assertEquals("test 2", resourcePut.getUserDefinedFields().getUserDefinedField2());
    assertEquals("test 3", resourcePut.getUserDefinedFields().getUserDefinedField3());
    assertEquals("test 4", resourcePut.getUserDefinedFields().getUserDefinedField4());
    assertEquals("test 5", resourcePut.getUserDefinedFields().getUserDefinedField5());
  }

  @Test
  public void shouldCreateRequestToUpdateCustomEmbargoPeriodForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withCustomEmbargoPeriod(new EmbargoPeriod()
            .withEmbargoUnit(EmbargoUnit.YEARS)
            .withEmbargoValue(10))), resourceData);
      assertEquals("Years", resourcePut.getCustomEmbargoPeriod().getEmbargoUnit());
      assertEquals(10, resourcePut.getCustomEmbargoPeriod().getEmbargoValue());
  }

  @Test
  public void shouldCreateRequestToUpdateUrlForCustomResource() {
    CustomerResources.CustomerResourcesBuilder customerResourcesBuilder = resourceData.getCustomerResourcesList().get(0).toBuilder();
    CustomerResources resources = customerResourcesBuilder.isPackageCustom(true).build();
    Title title = resourceData.toBuilder().customerResourcesList(Collections.singletonList(resources)).build();

    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withUrl("test url")), title);
    assertEquals("test url", resourcePut.getUrl());
  }

  @Test
  public void shouldCreateRequestWithOldDataWhenUpdateFieldsAreMissing() {
    CustomerResources.CustomerResourcesBuilder customerResourcesBuilder = resourceData.getCustomerResourcesList().get(0).toBuilder();
    CustomerResources resources = customerResourcesBuilder.isPackageCustom(true).build();
    Title title = resourceData.toBuilder().customerResourcesList(Collections.singletonList(resources)).build();

    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourcePutDataAttributes()), title);
    assertEquals(OLD_PROXY_ID, resourcePut.getProxy().getId());
    assertEquals(OLD_COVERAGE_STATEMENT, resourcePut.getCoverageStatement());
    assertEquals(OLD_URL, resourcePut.getUrl());
    assertEquals(OLD_VISIBILITY_DATA, resourcePut.getIsHidden());
    assertEquals(OLD_EMBARGO_UNIT, resourcePut.getCustomEmbargoPeriod().getEmbargoUnit());
    assertEquals(OLD_EMBARGO_VALUE, resourcePut.getCustomEmbargoPeriod().getEmbargoValue());
  }
}
