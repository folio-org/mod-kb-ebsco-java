package org.folio.rest.converter.packages;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackageAltName;
import org.folio.rest.jaxrs.model.PackagePostData;
import org.folio.rest.jaxrs.model.PackagePostDataAttributes;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.ProxyDto;
import org.junit.jupiter.api.Test;

class CustomPackagePostRequestConverterTest {

  private final CustomPackagePostRequestConverter converter = new CustomPackagePostRequestConverter();

  @Test
  void shouldMapPackageName() {
    var result = converter.convert(buildRequest(new PackagePostDataAttributes()
      .withName("My Package")
      .withContentType(ContentType.UNKNOWN)));
    assertEquals("My Package", result.getPackageName());
  }

  @Test
  void shouldMapCustomDescription() {
    var result = converter.convert(buildRequest(new PackagePostDataAttributes()
      .withContentType(ContentType.UNKNOWN)
      .withCustomDescription("Some description")));
    assertEquals("Some description", result.getCustomDescription());
  }

  @Test
  void shouldMapCustomDisplayName() {
    var result = converter.convert(buildRequest(new PackagePostDataAttributes()
      .withContentType(ContentType.UNKNOWN)
      .withCustomDisplayName("Display Name")));
    assertEquals("Display Name", result.getCustomDisplayName());
  }

  @Test
  void shouldMapFreeAccess() {
    var result = converter.convert(buildRequest(new PackagePostDataAttributes()
      .withContentType(ContentType.UNKNOWN)
      .withIsFreeAccess(true)));
    assertTrue(result.getPackageFreeAccess());
  }

  @Test
  void shouldMapContentTypeToRmapiCode() {
    var result = converter.convert(buildRequest(new PackagePostDataAttributes()
      .withContentType(ContentType.AGGREGATED_FULL_TEXT)));
    assertEquals(1, result.getContentType());
  }

  @Test
  void shouldMapStreamingMediaContentType() {
    var result = converter.convert(buildRequest(new PackagePostDataAttributes()
      .withContentType(ContentType.STREAMING_MEDIA)));
    assertEquals(8, result.getContentType());
  }

  @Test
  void shouldUseDefaultContentTypeCodeForUnknown() {
    var result = converter.convert(buildRequest(new PackagePostDataAttributes()
      .withContentType(ContentType.UNKNOWN)));
    assertEquals(6, result.getContentType());
  }

  @Test
  void shouldMapCustomCoverage() {
    var result = converter.convert(buildRequest(new PackagePostDataAttributes()
      .withContentType(ContentType.UNKNOWN)
      .withCustomCoverage(new Coverage()
        .withBeginCoverage("2020-01-01")
        .withEndCoverage("2021-12-31"))));
    assertEquals("2020-01-01", result.getCoverage().getBeginCoverage());
    assertEquals("2021-12-31", result.getCoverage().getEndCoverage());
  }

  @Test
  void shouldReturnNullCoverageWhenNotProvided() {
    var result = converter.convert(buildRequest(new PackagePostDataAttributes()
      .withContentType(ContentType.UNKNOWN)));
    assertNull(result.getCoverage());
  }

  @Test
  void shouldMapProxy() {
    var result = converter.convert(buildRequest(new PackagePostDataAttributes()
      .withContentType(ContentType.UNKNOWN)
      .withProxy(new ProxyDto().withId("proxy-id"))));
    assertEquals("proxy-id", result.getProxy().getId());
    assertFalse(result.getProxy().getInherited());
  }

  @Test
  void shouldReturnNullProxyWhenNotProvided() {
    var result = converter.convert(buildRequest(new PackagePostDataAttributes()
      .withContentType(ContentType.UNKNOWN)));
    assertNull(result.getProxy());
  }

  @Test
  void shouldMapCustomAltNames() {
    var result = converter.convert(buildRequest(new PackagePostDataAttributes()
      .withContentType(ContentType.UNKNOWN)
      .withCustomAltNames(List.of(
        new PackageAltName().withId(1).withAltName("Alt One"),
        new PackageAltName().withId(2).withAltName("Alt Two")))));
    assertEquals(2, result.getCustomAltNames().size());
    assertEquals(1, result.getCustomAltNames().get(0).id());
    assertEquals("Alt One", result.getCustomAltNames().get(0).altName());
    assertEquals(2, result.getCustomAltNames().get(1).id());
    assertEquals("Alt Two", result.getCustomAltNames().get(1).altName());
  }

  @Test
  void shouldMapEmptyAltNamesToEmptyList() {
    var result = converter.convert(buildRequest(new PackagePostDataAttributes()
      .withContentType(ContentType.UNKNOWN)
      .withCustomAltNames(emptyList())));
    assertTrue(result.getCustomAltNames().isEmpty());
  }

  @Test
  void shouldMapSingleAltName() {
    var result = converter.convert(buildRequest(new PackagePostDataAttributes()
      .withContentType(ContentType.UNKNOWN)
      .withCustomAltNames(singletonList(
        new PackageAltName().withId(5).withAltName("Single Alt")))));
    assertEquals(1, result.getCustomAltNames().size());
    assertEquals("Single Alt", result.getCustomAltNames().getFirst().altName());
  }

  private PackagePostRequest buildRequest(PackagePostDataAttributes attributes) {
    return new PackagePostRequest()
      .withData(new PackagePostData()
        .withType(PackagePostData.Type.PACKAGES)
        .withAttributes(attributes));
  }
}
