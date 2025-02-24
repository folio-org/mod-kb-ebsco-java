package org.folio.rest.converter.titles;

import static org.folio.rest.impl.ResourcesTestData.OLD_PROXY_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.folio.holdingsiq.model.ResourcePut;
import org.folio.rest.converter.common.attr.ContributorsConverterPair;
import org.folio.rest.converter.common.attr.IdentifiersConverterPair;
import org.folio.rest.impl.ResourcesTestData;
import org.folio.rest.jaxrs.model.PublicationType;
import org.folio.rest.jaxrs.model.TitlePutData;
import org.folio.rest.jaxrs.model.TitlePutDataAttributes;
import org.folio.rest.jaxrs.model.TitlePutRequest;
import org.junit.Test;

public class TitlePutRequestConverterTest {
  private final TitlePutRequestConverter converter = new TitlePutRequestConverter(
    new IdentifiersConverterPair.ToRmApi(),
    new ContributorsConverterPair.ToRmApi());

  @Test
  public void shouldCreateRequestToUpdatePublicationTypeForCustomResource() {
    TitlePutRequest request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setPublicationType(PublicationType.BOOK_SERIES);
    ResourcePut resourcePut = converter.convertToRmApiCustomResourcePutRequest(request,
      ResourcesTestData.createResourceData().getTitle().getCustomerResourcesList().getFirst());
    assertEquals("bookseries", resourcePut.getPubType());
    //from ProxyUrl to Proxy
    assertEquals(OLD_PROXY_ID, resourcePut.getProxy().getId());
    assertFalse(resourcePut.getProxy().getInherited());
  }

  @Test
  public void shouldCreateRequestToUpdateIsPeerReviewedForCustomResource() {
    TitlePutRequest request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setIsPeerReviewed(false);
    ResourcePut resourcePut = converter.convertToRmApiCustomResourcePutRequest(request,
      ResourcesTestData.createResourceData().getTitle().getCustomerResourcesList().getFirst());
    assertFalse(resourcePut.getIsPeerReviewed());
  }

  @Test
  public void shouldCreateRequestToUpdateResourceNameForCustomResource() {
    TitlePutRequest request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setName("test name");
    ResourcePut resourcePut = converter.convertToRmApiCustomResourcePutRequest(request,
      ResourcesTestData.createResourceData().getTitle().getCustomerResourcesList().getFirst());
    assertEquals("test name", resourcePut.getTitleName());
  }

  @Test
  public void shouldCreateRequestToUpdatePublisherNameForCustomResource() {
    TitlePutRequest request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setPublisherName("test pub name");
    ResourcePut resourcePut = converter.convertToRmApiCustomResourcePutRequest(request,
      ResourcesTestData.createResourceData().getTitle().getCustomerResourcesList().getFirst());
    assertEquals("test pub name", resourcePut.getPublisherName());
  }

  @Test
  public void shouldCreateRequestToUpdateEditionForCustomResource() {
    TitlePutRequest request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setEdition("test edition");
    ResourcePut resourcePut = converter.convertToRmApiCustomResourcePutRequest(request,
      ResourcesTestData.createResourceData().getTitle().getCustomerResourcesList().getFirst());
    assertEquals("test edition", resourcePut.getEdition());
  }

  @Test
  public void shouldCreateRequestToUpdateDescriptionForCustomResource() {
    TitlePutRequest request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setDescription("test description");
    ResourcePut resourcePut = converter.convertToRmApiCustomResourcePutRequest(request,
      ResourcesTestData.createResourceData().getTitle().getCustomerResourcesList().getFirst());
    assertEquals("test description", resourcePut.getDescription());
  }

  private TitlePutRequest createEmptyTitlePutRequest() {
    TitlePutRequest request = new TitlePutRequest();
    request.setData(new TitlePutData());
    request.getData().setAttributes(new TitlePutDataAttributes());
    return request;
  }
}

