package org.folio.rest.converter.resources;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.List.of;
import static org.folio.util.ResourcesTestUtil.getResourcePutRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.ProxyUrl;
import org.folio.holdingsiq.model.Resource;
import org.folio.holdingsiq.model.UserDefinedFields;
import org.folio.holdingsiq.model.Visibility;
import org.folio.rest.jaxrs.model.EmbargoPeriod;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.folio.rest.jaxrs.model.ProxyDto;
import org.folio.rest.jaxrs.model.ResourcePutDataAttributes;
import org.folio.rest.jaxrs.model.Visibility.Category;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourceRequestConverterTest {

  private static final boolean OLD_VISIBILITY_DATA = true;
  private static final int OLD_EMBARGO_VALUE = 5;
  private static final String OLD_BEGIN_COVERAGE = "2002-10-10";
  private static final String OLD_COVERAGE_STATEMENT = "statement";
  private static final String OLD_EMBARGO_UNIT = "Day";
  private static final String OLD_END_COVERAGE = "2003-10-10";
  private static final String OLD_PROXY_ID = "<n>";
  private static final String OLD_URL = "https://example.com";

  private final ResourceRequestConverter resourcesConverter = new ResourceRequestConverter();

  private Resource resourceData;

  @BeforeEach
  void setUp() {
    resourceData = Resource.builder()
      .contributorsList(emptyList())
      .coverageStatement(OLD_COVERAGE_STATEMENT)
      .isSelected(false)
      .visibilityDetails(of(new Visibility("PF", OLD_VISIBILITY_DATA, "")))
      .customCoverageList(singletonList(CoverageDates.builder()
        .beginCoverage(OLD_BEGIN_COVERAGE).endCoverage(OLD_END_COVERAGE).build()))
      .customEmbargoPeriod(org.folio.holdingsiq.model.EmbargoPeriod.builder()
        .embargoUnit(OLD_EMBARGO_UNIT).embargoValue(OLD_EMBARGO_VALUE).build())
      .proxy(ProxyUrl.builder().proxiedUrl(OLD_URL)
        .id(OLD_PROXY_ID).inherited(true).build())
      .url(OLD_URL)
      .userDefinedFields(UserDefinedFields.builder().build())
      .identifiersList(emptyList())
      .subjectsList(emptyList())
      .titleId(1)
      .build();
  }

  @Test
  void shouldCreateRequestToSelectManagedResource() {
    var resourcePut = resourcesConverter.convertToRmApiResourcePutRequest(getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)), resourceData);
    assertTrue(resourcePut.getIsSelected());
  }

  @Test
  void shouldCreateRequestToSelectCustomResource() {
    var resourcePut = resourcesConverter.convertToRmApiCustomResourcePutRequest(getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)), resourceData);
    assertTrue(resourcePut.getIsSelected());
  }

  @Test
  void shouldCreateRequestToUpdateProxyForManagedResource() {
    var resourcePut = resourcesConverter.convertToRmApiResourcePutRequest(getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withProxy(new ProxyDto()
          .withId("test-proxy-id"))), resourceData);
    assertEquals("test-proxy-id", resourcePut.getProxy().getId());
  }

  @Test
  void shouldCreateRequestToUpdateProxyForCustomResource() {
    var resourcePut = resourcesConverter.convertToRmApiCustomResourcePutRequest(getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withProxy(new ProxyDto()
          .withId("test-proxy-id"))), resourceData);
    assertEquals("test-proxy-id", resourcePut.getProxy().getId());
  }

  @Test
  void shouldCreateRequestToUpdateUserDefineFieldsForCustomResource() {
    var resourcePut = resourcesConverter.convertToRmApiCustomResourcePutRequest(getResourcePutRequest(
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
  void shouldCreateRequestToUpdateIsHiddenForCustomResource() {
    var resourcePut = resourcesConverter.convertToRmApiCustomResourcePutRequest(getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withVisibilityData(new VisibilityData()
          .withIsHidden(true))), resourceData);
    assertEquals(allCategories(true), resourcePut.getVisibilityDetails());
  }

  @Test
  void shouldCreateRequestToUpdateVisibilityPerCategory() {
    var resourcePut = resourcesConverter.convertToRmApiResourcePutRequest(getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withVisibility(of(
          new org.folio.rest.jaxrs.model.Visibility().withCategory(Category.PF).withHidden(true),
          new org.folio.rest.jaxrs.model.Visibility().withCategory(Category.FTF).withHidden(false),
          new org.folio.rest.jaxrs.model.Visibility().withCategory(Category.MARC).withHidden(true)))), resourceData);
    assertEquals(of(new Visibility("PF", true, "Hidden by Customer"), new Visibility("FTF", false, null),
      new Visibility("MARC", true, "Hidden by Customer")), resourcePut.getVisibilityDetails());
  }

  @Test
  void shouldCreateRequestToUpdateOnlySentVisibilityCategories() {
    var resourcePut = resourcesConverter.convertToRmApiCustomResourcePutRequest(getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withVisibility(of(
          new org.folio.rest.jaxrs.model.Visibility().withCategory(Category.MARC).withHidden(true)))), resourceData);
    assertEquals(of(new Visibility("MARC", true, "Hidden by Customer")), resourcePut.getVisibilityDetails());
  }

  @Test
  void shouldPreferVisibilityPerCategoryOverVisibilityData() {
    var resourcePut = resourcesConverter.convertToRmApiResourcePutRequest(getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withVisibilityData(new VisibilityData().withIsHidden(true))
        .withVisibility(of(
          new org.folio.rest.jaxrs.model.Visibility().withCategory(Category.PF).withHidden(false)))), resourceData);
    assertEquals(of(new Visibility("PF", false, null)), resourcePut.getVisibilityDetails());
  }

  @Test
  void shouldNotSendVisibilityWhenRequestDoesNotSetIt() {
    var resourcePut = resourcesConverter.convertToRmApiResourcePutRequest(getResourcePutRequest(
      new ResourcePutDataAttributes().withIsSelected(true)), resourceData);
    assertNull(resourcePut.getVisibilityDetails());
  }

  @Test
  void shouldCreateRequestToUpdateCoverageStatementForManagedResource() {
    var resourcePut = resourcesConverter.convertToRmApiResourcePutRequest(getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withCoverageStatement("test coverage stmt")), resourceData);
    assertEquals("test coverage stmt", resourcePut.getCoverageStatement());
  }

  @Test
  void shouldCreateRequestToUpdateCoverageStatementForCustomResource() {
    var resourcePut = resourcesConverter.convertToRmApiCustomResourcePutRequest(getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withCoverageStatement("test coverage stmt")), resourceData);
    assertEquals("test coverage stmt", resourcePut.getCoverageStatement());
  }

  @Test
  void shouldCreateRequestToUpdateIsHiddenForManagedResource() {
    var resourcePut = resourcesConverter.convertToRmApiResourcePutRequest(getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withVisibilityData(new VisibilityData()
          .withIsHidden(false))), resourceData);
    assertEquals(allCategories(false), resourcePut.getVisibilityDetails());
  }

  @Test
  void shouldCreateRequestToUpdateCustomEmbargoPeriodForManagedResource() {
    var resourcePut = resourcesConverter.convertToRmApiResourcePutRequest(getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withCustomEmbargoPeriod(new EmbargoPeriod()
          .withEmbargoUnit(EmbargoUnit.DAYS)
          .withEmbargoValue(10))), resourceData);
    assertEquals("Days", resourcePut.getCustomEmbargoPeriod().getEmbargoUnit());
    assertEquals(10, resourcePut.getCustomEmbargoPeriod().getEmbargoValue());
  }

  @Test
  void shouldCreateRequestToUpdateUserDefinedFieldsForManagedResource() {
    var resourcePut = resourcesConverter.convertToRmApiResourcePutRequest(getResourcePutRequest(
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
  void shouldCreateRequestToUpdateCustomEmbargoPeriodForCustomResource() {
    var resourcePut = resourcesConverter.convertToRmApiCustomResourcePutRequest(getResourcePutRequest(
      new ResourcePutDataAttributes()
        .withIsSelected(true)
        .withCustomEmbargoPeriod(new EmbargoPeriod()
          .withEmbargoUnit(EmbargoUnit.YEARS)
          .withEmbargoValue(10))), resourceData);
    assertEquals("Years", resourcePut.getCustomEmbargoPeriod().getEmbargoUnit());
    assertEquals(10, resourcePut.getCustomEmbargoPeriod().getEmbargoValue());
  }

  @Test
  void shouldCreateRequestToUpdateUrlForCustomResource() {
    var customResource = resourceData.toBuilder().isPackageCustom(true).build();

    var resourcePut =
      resourcesConverter.convertToRmApiCustomResourcePutRequest(getResourcePutRequest(
        new ResourcePutDataAttributes()
          .withIsSelected(true)
          .withUrl("test url")), customResource);
    assertEquals("test url", resourcePut.getUrl());
  }

  @Test
  void shouldCreateRequestWithOldDataExceptEmbargoWhenUpdateFieldsAreMissing() {
    var customResource = resourceData.toBuilder().isPackageCustom(true).build();

    var resourcePut = resourcesConverter.convertToRmApiCustomResourcePutRequest(
      getResourcePutRequest(new ResourcePutDataAttributes()),
      customResource
    );
    assertEquals(OLD_PROXY_ID, resourcePut.getProxy().getId());
    assertFalse(resourcePut.getProxy().getInherited());
    assertEquals(OLD_COVERAGE_STATEMENT, resourcePut.getCoverageStatement());
    assertEquals(OLD_URL, resourcePut.getUrl());
    // visibility is not sent when not requested, so HoldingsIQ keeps the current visibility
    assertNull(resourcePut.getVisibilityDetails());
    assertNull(resourcePut.getCustomEmbargoPeriod());
  }

  private static List<Visibility> allCategories(boolean hidden) {
    var reason = hidden ? "Hidden by Customer" : null;
    return of(new Visibility("PF", hidden, reason), new Visibility("FTF", hidden, reason),
      new Visibility("MARC", hidden, reason));
  }
}
