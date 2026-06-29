package org.folio.rest.converter.titles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.EmbargoPeriod;
import org.folio.holdingsiq.model.ProxyUrl;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.UserDefinedFields;
import org.folio.holdingsiq.model.VisibilityInfo;
import org.folio.rest.converter.common.attr.ContributorsConverterPair;
import org.folio.rest.converter.common.attr.IdentifiersConverterPair;
import org.folio.rest.jaxrs.model.PublicationType;
import org.folio.rest.jaxrs.model.TitlePutData;
import org.folio.rest.jaxrs.model.TitlePutDataAttributes;
import org.folio.rest.jaxrs.model.TitlePutRequest;
import org.folio.rmapi.result.ResourceResult;
import org.junit.jupiter.api.Test;

class TitlePutRequestConverterTest {

  private static final boolean OLD_VISIBILITY_DATA = true;
  private static final int OLD_EMBARGO_VALUE = 5;
  private static final String OLD_BEGIN_COVERAGE = "2002-10-10";
  private static final String OLD_COVERAGE_STATEMENT = "statement";
  private static final String OLD_EMBARGO_UNIT = "Day";
  private static final String OLD_END_COVERAGE = "2003-10-10";
  private static final String OLD_PROXY_ID = "<n>";
  private static final String OLD_URL = "http://example.com";

  private final TitlePutRequestConverter converter = new TitlePutRequestConverter(
    new IdentifiersConverterPair.ToRmApi(),
    new ContributorsConverterPair.ToRmApi());

  @Test
  void shouldCreateRequestToUpdatePublicationTypeForCustomResource() {
    var request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setPublicationType(PublicationType.BOOK_SERIES);
    var resourcePut = converter.convertToRmApiCustomResourcePutRequest(request,
      createResourceData().getTitle().getCustomerResourcesList().getFirst());
    assertEquals("bookseries", resourcePut.getPubType());
    //from ProxyUrl to Proxy
    assertEquals(OLD_PROXY_ID, resourcePut.getProxy().getId());
    assertFalse(resourcePut.getProxy().getInherited());
  }

  @Test
  void shouldCreateRequestToUpdateIsPeerReviewedForCustomResource() {
    var request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setIsPeerReviewed(false);
    var resourcePut = converter.convertToRmApiCustomResourcePutRequest(request,
      createResourceData().getTitle().getCustomerResourcesList().getFirst());
    assertFalse(resourcePut.getIsPeerReviewed());
  }

  @Test
  void shouldCreateRequestToUpdateResourceNameForCustomResource() {
    var request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setName("test name");
    var resourcePut = converter.convertToRmApiCustomResourcePutRequest(request,
      createResourceData().getTitle().getCustomerResourcesList().getFirst());
    assertEquals("test name", resourcePut.getTitleName());
  }

  @Test
  void shouldCreateRequestToUpdatePublisherNameForCustomResource() {
    var request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setPublisherName("test pub name");
    var resourcePut = converter.convertToRmApiCustomResourcePutRequest(request,
      createResourceData().getTitle().getCustomerResourcesList().getFirst());
    assertEquals("test pub name", resourcePut.getPublisherName());
  }

  @Test
  void shouldCreateRequestToUpdateEditionForCustomResource() {
    var request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setEdition("test edition");
    var resourcePut = converter.convertToRmApiCustomResourcePutRequest(request,
      createResourceData().getTitle().getCustomerResourcesList().getFirst());
    assertEquals("test edition", resourcePut.getEdition());
  }

  @Test
  void shouldCreateRequestToUpdateDescriptionForCustomResource() {
    var request = createEmptyTitlePutRequest();
    request.getData().getAttributes().setDescription("test description");
    var resourcePut = converter.convertToRmApiCustomResourcePutRequest(request,
      createResourceData().getTitle().getCustomerResourcesList().getFirst());
    assertEquals("test description", resourcePut.getDescription());
  }

  private ResourceResult createResourceData() {
    var title = Title.builder()
      .contributorsList(Collections.emptyList())
      .customerResourcesList(Collections.singletonList(CustomerResources.builder()
        .coverageStatement(OLD_COVERAGE_STATEMENT)
        .isSelected(false)
        .visibilityData(VisibilityInfo.builder()
          .isHidden(OLD_VISIBILITY_DATA).build())
        .customCoverageList(Collections.singletonList(CoverageDates.builder()
          .beginCoverage(OLD_BEGIN_COVERAGE).endCoverage(OLD_END_COVERAGE).build()))
        .customEmbargoPeriod(EmbargoPeriod.builder()
          .embargoUnit(OLD_EMBARGO_UNIT).embargoValue(OLD_EMBARGO_VALUE).build())
        .proxy(ProxyUrl.builder().proxiedUrl(OLD_URL)
          .id(OLD_PROXY_ID).inherited(true).build())
        .url(OLD_URL)
        .userDefinedFields(UserDefinedFields.builder().build())
        .build()
      ))
      .identifiersList(Collections.emptyList())
      .subjectsList(Collections.emptyList())
      .titleId(1)
      .build();
    return new ResourceResult(title, null, null, false);
  }

  private TitlePutRequest createEmptyTitlePutRequest() {
    TitlePutRequest request = new TitlePutRequest();
    request.setData(new TitlePutData());
    request.getData().setAttributes(new TitlePutDataAttributes());
    return request;
  }
}

