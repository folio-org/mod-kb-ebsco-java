package org.folio.rest.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.folio.rest.impl.ResourcesTestData;
import org.folio.rest.jaxrs.model.EmbargoPeriod;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.Proxy;
import org.folio.rest.jaxrs.model.PublicationType;
import org.folio.rest.jaxrs.model.ResourceDataAttributes;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.folio.rmapi.model.ResourcePut;
import org.junit.Test;

public class ResourcesConverterTest {
  private ResourcesConverter resourcesConverter = new ResourcesConverter();

  @Test
  public void shouldCreateRequestToSelectManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)));
    assertTrue(resourcePut.getIsSelected());
  }
  
  @Test
  public void shouldCreateRequestToSelectCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)));
    assertTrue(resourcePut.getIsSelected());
  }

  @Test
  public void shouldCreateRequestToUpdateProxyForManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withProxy(new Proxy()
          .withId("test-proxy-id"))));
    assertEquals("test-proxy-id", resourcePut.getProxy().getId());
  }
  
  @Test
  public void shouldCreateRequestToUpdateProxyForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withProxy(new Proxy()
          .withId("test-proxy-id"))));
    assertEquals("test-proxy-id", resourcePut.getProxy().getId());
  }
  
  @Test
  public void shouldCreateRequestToUpdateIsHiddenForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withVisibilityData(new VisibilityData()
          .withIsHidden(true))));
    assertTrue(resourcePut.getIsHidden());
  }
  
  @Test
  public void shouldCreateRequestToUpdateCoverageStatementForManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withCoverageStatement("test coverage stmt")));
      assertEquals("test coverage stmt", resourcePut.getCoverageStatement());
  }
  
  @Test
  public void shouldCreateRequestToUpdateCoverageStatementForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withCoverageStatement("test coverage stmt")));
    assertEquals("test coverage stmt", resourcePut.getCoverageStatement());
  }
  
  @Test
  public void shouldCreateRequestToUpdateIsHiddenForManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withVisibilityData(new VisibilityData()
            .withIsHidden(false))));
      assertFalse(resourcePut.getIsHidden());
  }
  
  @Test
  public void shouldCreateRequestToUpdateCustomEmbargoPeriodForManagedResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPIResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withCustomEmbargoPeriod(new EmbargoPeriod()
            .withEmbargoUnit(EmbargoUnit.DAYS)
            .withEmbargoValue(10))));
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
            .withEmbargoValue(10))));
      assertEquals("Years", resourcePut.getCustomEmbargoPeriod().getEmbargoUnit());
      assertEquals(10, (long)resourcePut.getCustomEmbargoPeriod().getEmbargoValue());
  }

  @Test
  public void shouldCreateRequestToUpdatePublicationTypeForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withPublicationType(PublicationType.BOOK_SERIES)));
      assertEquals("Book Series", resourcePut.getPubType());
  }
  
  @Test
  public void shouldCreateRequestToUpdateIsPeerReviewedForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withIsPeerReviewed(false)));
    assertFalse(resourcePut.getIsPeerReviewed());
  }

  @Test
  public void shouldCreateRequestToUpdateResourceNameForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withName("test name")));
    assertEquals("test name", resourcePut.getTitleName());
  }

  @Test
  public void shouldCreateRequestToUpdatePublisherNameForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withPublisherName("test pub name")));
    assertEquals("test pub name", resourcePut.getPublisherName());
  }

  @Test
  public void shouldCreateRequestToUpdateEditionForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withEdition("test edition")));
    assertEquals("test edition", resourcePut.getEdition());
  }

  @Test
  public void shouldCreateRequestToUpdateDescriptionForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withDescription("test description")));
    assertEquals("test description", resourcePut.getDescription());
  }

  @Test
  public void shouldCreateRequestToUpdateUrlForCustomResource() {
    ResourcePut resourcePut = resourcesConverter.convertToRMAPICustomResourcePutRequest(ResourcesTestData.getResourcePutRequest(
      new ResourceDataAttributes()
        .withIsSelected(true)
        .withUrl("test url")));
    assertEquals("test url", resourcePut.getUrl());
  }
}
