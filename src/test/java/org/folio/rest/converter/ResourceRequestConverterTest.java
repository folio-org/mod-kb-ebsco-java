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
  private ResourceRequestConverter resourcesConverter = new ResourceRequestConverter();
  private ResourceResult resourceData;

  @Before
  public void setUp() {
    Title title = Title.builder()
      .contributorsList(Collections.emptyList())
      .customerResourcesList(Collections.singletonList(CustomerResources.builder()
        .coverageStatement("statement")
        .isSelected(false)
        .visibilityData(VisibilityInfo.builder()
          .isHidden(true).build())
        .customCoverageList(Collections.singletonList(CoverageDates.builder()
          .beginCoverage("2002-10-10").endCoverage("2003-10-10").build()))
        .customEmbargoPeriod(org.folio.rmapi.model.EmbargoPeriod.builder()
          .embargoUnit("Day").embargoValue(5).build())
        .proxy(org.folio.rmapi.model.Proxy.builder()
          .id("<n>").inherited(true).build())
        .url("http://example.com")
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
}
