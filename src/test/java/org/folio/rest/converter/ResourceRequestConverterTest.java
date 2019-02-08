package org.folio.rest.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.folio.rest.converter.resources.ResourceRequestConverter;
import org.folio.rest.impl.ResourcesTestData;
import org.folio.rest.jaxrs.model.EmbargoPeriod;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.PublicationType;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.folio.rmapi.model.CoverageDates;
import org.folio.rmapi.model.CustomerResources;
import org.folio.rmapi.model.ResourcePut;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.model.VisibilityInfo;
import org.folio.rmapi.result.ResourceResult;
import org.junit.Before;
import org.junit.Test;

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
  private ResourceResult resourceData;

  @Before
  public void setUp() {
    Title title = Title.builder()
      .contributorsList(Collections.emptyList())
      .customerResourcesList(Collections.singletonList(CustomerResources.builder()
        .coverageStatement(OLD_COVERAGE_STATEMENT)
        .isSelected(false)
        .visibilityData(VisibilityInfo.builder()
          .isHidden(OLD_VISIBILITY_DATA).build())
        .customCoverageList(Collections.singletonList(CoverageDates.builder()
          .beginCoverage(OLD_BEGIN_COVERAGE).endCoverage(OLD_END_COVERAGE).build()))
        .customEmbargoPeriod(org.folio.rmapi.model.EmbargoPeriod.builder()
          .embargoUnit(OLD_EMBARGO_UNIT).embargoValue(OLD_EMBARGO_VALUE).build())
        .proxy(org.folio.rmapi.model.Proxy.builder()
          .id(OLD_PROXY_ID).inherited(true).build())
        .url(OLD_URL)
        .build()
      ))
      .identifiersList(Collections.emptyList())
      .subjectsList(Collections.emptyList())
      .titleId(1)
      .build();
    resourceData = new ResourceResult(
      title, null, null, false);
  }

  @Test
  public void shouldCreateRequestToSelectManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)), resourceData);
    assertTrue(resourcePut.getIsSelected());
  }

  @Test
  public void shouldCreateRequestToSelectCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)), resourceData);
    assertTrue(resourcePut.getIsSelected());
  }

  @Test
  public void shouldCreateRequestToUpdateProxyForManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withProxy(new Proxy()
          .withId("test-proxy-id"))), resourceData);
    assertEquals("test-proxy-id", resourcePut.getProxy().getId());
  }

  @Test
  public void shouldCreateRequestToUpdateProxyForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withProxy(new Proxy()
          .withId("test-proxy-id"))), resourceData);
    assertEquals("test-proxy-id", resourcePut.getProxy().getId());
  }

  @Test
  public void shouldCreateRequestToUpdateIsHiddenForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withVisibilityData(new VisibilityData()
          .withIsHidden(true))), resourceData);
    assertTrue(resourcePut.getIsHidden());
  }

  @Test
  public void shouldCreateRequestToUpdateCoverageStatementForManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withCoverageStatement("test coverage stmt")), resourceData);
      assertEquals("test coverage stmt", resourcePut.getCoverageStatement());
  }

  @Test
  public void shouldCreateRequestToUpdateCoverageStatementForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withCoverageStatement("test coverage stmt")), resourceData);
    assertEquals("test coverage stmt", resourcePut.getCoverageStatement());
  }

  @Test
  public void shouldCreateRequestToUpdateIsHiddenForManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withVisibilityData(new VisibilityData()
            .withIsHidden(false))), resourceData);
      assertFalse(resourcePut.getIsHidden());
  }

  @Test
  public void shouldCreateRequestToUpdateCustomEmbargoPeriodForManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withCustomEmbargoPeriod(new EmbargoPeriod()
            .withEmbargoUnit(EmbargoUnit.DAYS)
            .withEmbargoValue(10))), resourceData);
      assertEquals("Days", resourcePut.getCustomEmbargoPeriod().getEmbargoUnit());
      assertEquals(10, (long)resourcePut.getCustomEmbargoPeriod().getEmbargoValue());
  }

  @Test
  public void shouldCreateRequestToUpdateCustomEmbargoPeriodForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withCustomEmbargoPeriod(new EmbargoPeriod()
            .withEmbargoUnit(EmbargoUnit.YEARS)
            .withEmbargoValue(10))), resourceData);
      assertEquals("Years", resourcePut.getCustomEmbargoPeriod().getEmbargoUnit());
      assertEquals(10, (long)resourcePut.getCustomEmbargoPeriod().getEmbargoValue());
  }

  @Test
  public void shouldCreateRequestToUpdatePublicationTypeForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withPublicationType(PublicationType.BOOK_SERIES)), resourceData);
      assertEquals("Book Series", resourcePut.getPubType());
  }

  @Test
  public void shouldCreateRequestToUpdateIsPeerReviewedForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withIsPeerReviewed(false)), resourceData);
    assertFalse(resourcePut.getIsPeerReviewed());
  }

  @Test
  public void shouldCreateRequestToUpdateResourceNameForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withName("test name")), resourceData);
    assertEquals("test name", resourcePut.getTitleName());
  }

  @Test
  public void shouldCreateRequestToUpdatePublisherNameForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withPublisherName("test pub name")), resourceData);
    assertEquals("test pub name", resourcePut.getPublisherName());
  }

  @Test
  public void shouldCreateRequestToUpdateEditionForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withEdition("test edition")), resourceData);
    assertEquals("test edition", resourcePut.getEdition());
  }

  @Test
  public void shouldCreateRequestToUpdateDescriptionForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withDescription("test description")), resourceData);
    assertEquals("test description", resourcePut.getDescription());
  }

  @Test
  public void shouldCreateRequestToUpdateUrlForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withUrl("test url")), resourceData);
    assertEquals("test url", resourcePut.getUrl());
  }

  @Test
  public void shouldCreateRequestWithOldDataWhenUpdateFieldsAreMissing() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()), resourceData);
    assertEquals(OLD_PROXY_ID, resourcePut.getProxy().getId());
    assertEquals(OLD_COVERAGE_STATEMENT, resourcePut.getCoverageStatement());
    assertEquals(OLD_URL, resourcePut.getUrl());
    assertEquals(OLD_VISIBILITY_DATA, resourcePut.getIsHidden());
    assertEquals(OLD_BEGIN_COVERAGE, resourcePut.getCustomCoverageList().get(0).getBeginCoverage());
    assertEquals(OLD_END_COVERAGE, resourcePut.getCustomCoverageList().get(0).getEndCoverage());
    assertEquals(OLD_EMBARGO_UNIT, resourcePut.getCustomEmbargoPeriod().getEmbargoUnit());
    assertEquals(OLD_EMBARGO_VALUE, resourcePut.getCustomEmbargoPeriod().getEmbargoValue());
  }
}
