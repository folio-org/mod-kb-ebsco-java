package org.folio.rest.converter.titles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import org.folio.holdingsiq.model.ResourcePut;
import org.folio.rest.converter.common.attr.ContributorsConverterPair;
import org.folio.rest.converter.common.attr.IdentifiersConverterPair;
import org.folio.rest.impl.ResourcesTestData;
import org.folio.rest.jaxrs.model.PublicationType;
import org.folio.rest.jaxrs.model.TitlePostDataAttributes;
import org.folio.rest.jaxrs.model.TitlePutData;
import org.folio.rest.jaxrs.model.TitlePutRequest;

public class TitlePutRequestConverterTest {
  private TitlePutRequestConverter converter = new TitlePutRequestConverter(
    new IdentifiersConverterPair.ToRMApi(),
    new ContributorsConverterPair.ToRMApi());

  @Test
  public void shouldCreateRequestToUpdatePublicationTypeForCustomResource() {
    TitlePutRequest request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setPublicationType(PublicationType.BOOK_SERIES);
    ResourcePut resourcePut = converter.convertToRMAPICustomResourcePutRequest(request,
      ResourcesTestData.createResourceData().getTitle().getCustomerResourcesList().get(0));
    assertEquals("bookseries", resourcePut.getPubType());
  }

  @Test
  public void shouldCreateRequestToUpdateIsPeerReviewedForCustomResource() {
    TitlePutRequest request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setIsPeerReviewed(false);
    ResourcePut resourcePut = converter.convertToRMAPICustomResourcePutRequest(request,
      ResourcesTestData.createResourceData().getTitle().getCustomerResourcesList().get(0));
    assertFalse(resourcePut.getIsPeerReviewed());
  }

  @Test
  public void shouldCreateRequestToUpdateResourceNameForCustomResource() {
    TitlePutRequest request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setName("test name");
    ResourcePut resourcePut = converter.convertToRMAPICustomResourcePutRequest(request,
      ResourcesTestData.createResourceData().getTitle().getCustomerResourcesList().get(0));
    assertEquals("test name", resourcePut.getTitleName());
  }

  @Test
  public void shouldCreateRequestToUpdatePublisherNameForCustomResource() {
    TitlePutRequest request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setPublisherName("test pub name");
    ResourcePut resourcePut = converter.convertToRMAPICustomResourcePutRequest(request,
      ResourcesTestData.createResourceData().getTitle().getCustomerResourcesList().get(0));
    assertEquals("test pub name", resourcePut.getPublisherName());
  }

  @Test
  public void shouldCreateRequestToUpdateEditionForCustomResource() {
    TitlePutRequest request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setEdition("test edition");
    ResourcePut resourcePut = converter.convertToRMAPICustomResourcePutRequest(request,
      ResourcesTestData.createResourceData().getTitle().getCustomerResourcesList().get(0));
    assertEquals("test edition", resourcePut.getEdition());
  }

  @Test
  public void shouldCreateRequestToUpdateDescriptionForCustomResource() {
    TitlePutRequest request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setDescription("test description");
    ResourcePut resourcePut = converter.convertToRMAPICustomResourcePutRequest(request,
      ResourcesTestData.createResourceData().getTitle().getCustomerResourcesList().get(0));
    assertEquals("test description", resourcePut.getDescription());
  }

  private TitlePutRequest createEmptyTitlePutRequest() {
    TitlePutRequest request = new TitlePutRequest();
    request.setData(new TitlePutData());
    request.getData().setAttributes(new TitlePostDataAttributes());
    return request;
  }
}

