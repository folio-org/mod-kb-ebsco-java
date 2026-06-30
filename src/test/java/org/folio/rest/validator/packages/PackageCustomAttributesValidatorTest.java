package org.folio.rest.validator.packages;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackageAltName;
import org.folio.rest.validator.packages.PackageCustomAttributesValidator.PackageCustomAttributes;
import org.junit.jupiter.api.Test;

class PackageCustomAttributesValidatorTest {

  private final PackageCustomAttributesValidator validator = new PackageCustomAttributesValidator();

  @Test
  void shouldThrowWhenCustomDescriptionExceedsMaxLength() {
    var customAttributes = withDescription("a".repeat(2001));
    assertThrows(InputValidationException.class, () -> validator.validate(customAttributes));
  }

  @Test
  void shouldNotThrowWhenCustomDescriptionIsExactlyMaxLength() {
    var customAttributes = withDescription("a".repeat(2000));
    assertDoesNotThrow(() -> validator.validate(customAttributes));
  }

  @Test
  void shouldNotThrowWhenCustomDescriptionContainsHtml() {
    var customAttributes = withDescription("<b>bold</b>");
    assertDoesNotThrow(() -> validator.validate(customAttributes));
  }

  @Test
  void shouldNotThrowWhenCustomDescriptionIsNull() {
    var customAttributes = withDescription(null);
    assertDoesNotThrow(() -> validator.validate(customAttributes));
  }

  @Test
  void shouldThrowWhenCustomDisplayNameExceedsMaxLength() {
    var customAttributes = withDisplayName("a".repeat(301));
    assertThrows(InputValidationException.class, () -> validator.validate(customAttributes));
  }

  @Test
  void shouldNotThrowWhenCustomDisplayNameIsExactlyMaxLength() {
    var customAttributes = withDisplayName("a".repeat(300));
    assertDoesNotThrow(() -> validator.validate(customAttributes));
  }

  @Test
  void shouldThrowWhenCustomDisplayNameContainsHtml() {
    var customAttributes = withDisplayName("<b>name</b>");
    assertThrows(InputValidationException.class, () -> validator.validate(customAttributes));
  }

  @Test
  void shouldNotThrowWhenCustomDisplayNameIsNull() {
    var customAttributes = withDisplayName(null);
    assertDoesNotThrow(() -> validator.validate(customAttributes));
  }

  @Test
  void shouldThrowWhenCustomAltNamesExceedMaxCount() {
    var altNames = IntStream.rangeClosed(1, 11)
      .mapToObj(i -> new PackageAltName().withAltName("name" + i))
      .toList();
    var customAttributes = withAltNames(altNames);
    assertThrows(InputValidationException.class, () -> validator.validate(customAttributes));
  }

  @Test
  void shouldNotThrowWhenCustomAltNamesCountIsExactlyMax() {
    var altNames = IntStream.rangeClosed(1, 10)
      .mapToObj(i -> new PackageAltName().withAltName("name" + i))
      .toList();
    var customAttributes = withAltNames(altNames);
    assertDoesNotThrow(() -> validator.validate(customAttributes));
  }

  @Test
  void shouldThrowWhenAltNameExceedsMaxLength() {
    var customAttributes = withAltNames(Collections.singletonList(
      new PackageAltName().withAltName("a".repeat(301))));
    assertThrows(InputValidationException.class, () -> validator.validate(customAttributes));
  }

  @Test
  void shouldThrowWhenAltNameContainsHtml() {
    var customAttributes = withAltNames(Collections.singletonList(
      new PackageAltName().withAltName("<em>alt</em>")));
    assertThrows(InputValidationException.class, () -> validator.validate(customAttributes));
  }

  @Test
  void shouldNotThrowWhenCustomAltNamesIsNull() {
    var customAttributes = withAltNames(null);
    assertDoesNotThrow(() -> validator.validate(customAttributes));
  }

  @Test
  void shouldThrowWhenUrlExceedsMaxLength() {
    var customAttributes = withUrl("a".repeat(501));
    assertThrows(InputValidationException.class, () -> validator.validate(customAttributes));
  }

  @Test
  void shouldNotThrowWhenUrlIsExactlyMaxLength() {
    var customAttributes = withUrl("https://example.com/" + "a".repeat(480));
    assertDoesNotThrow(() -> validator.validate(customAttributes));
  }

  @Test
  void shouldNotThrowWhenUrlIsValid() {
    var customAttributes = withUrl("https://example.com/path");
    assertDoesNotThrow(() -> validator.validate(customAttributes));
  }

  @Test
  void shouldThrowWhenUrlHasNoProtocol() {
    var customAttributes = withUrl("example.com");
    assertThrows(InputValidationException.class, () -> validator.validate(customAttributes));
  }

  @Test
  void shouldThrowWhenUrlIsInvalidFormat() {
    var customAttributes = withUrl("not a url");
    assertThrows(InputValidationException.class, () -> validator.validate(customAttributes));
  }

  @Test
  void shouldNotThrowWhenUrlIsNull() {
    var customAttributes = withUrl(null);
    assertDoesNotThrow(() -> validator.validate(customAttributes));
  }

  @Test
  void shouldNotThrowWhenCustomCoverageIsNull() {
    var customAttributes = withCoverage(null);
    assertDoesNotThrow(() -> validator.validate(customAttributes));
  }

  @Test
  void shouldNotThrowWhenBeginCoverageIsNull() {
    var customAttributes = withCoverage(new Coverage());
    assertDoesNotThrow(() -> validator.validate(customAttributes));
  }

  @Test
  void shouldThrowWhenBeginCoverageHasInvalidFormat() {
    var customAttributes = withCoverage(new Coverage().withBeginCoverage("not-a-date"));
    assertThrows(InputValidationException.class, () -> validator.validate(customAttributes));
  }

  @Test
  void shouldThrowWhenEndCoverageHasInvalidFormat() {
    var customAttributes = withCoverage(new Coverage()
      .withBeginCoverage("2020-01-01")
      .withEndCoverage("not-a-date"));
    assertThrows(InputValidationException.class, () -> validator.validate(customAttributes));
  }

  @Test
  void shouldThrowWhenBeginCoverageIsEmptyButEndCoverageIsNot() {
    var customAttributes = withCoverage(new Coverage()
      .withBeginCoverage("")
      .withEndCoverage("2020-12-31"));
    assertThrows(InputValidationException.class, () -> validator.validate(customAttributes));
  }

  @Test
  void shouldThrowWhenBeginCoverageIsAfterEndCoverage() {
    var customAttributes = withCoverage(new Coverage()
      .withBeginCoverage("2021-01-01")
      .withEndCoverage("2020-01-01"));
    assertThrows(InputValidationException.class, () -> validator.validate(customAttributes));
  }

  @Test
  void shouldNotThrowWhenCoverageHasValidDates() {
    var customAttributes = withCoverage(new Coverage()
      .withBeginCoverage("2020-01-01")
      .withEndCoverage("2021-12-31"));
    assertDoesNotThrow(() -> validator.validate(customAttributes));
  }

  @Test
  void shouldNotThrowWhenBeginCoverageIsValidAndEndIsNull() {
    var customAttributes = withCoverage(new Coverage()
      .withBeginCoverage("2020-01-01"));
    assertDoesNotThrow(() -> validator.validate(customAttributes));
  }

  @Test
  void shouldNotThrowWhenBothCoverageDatesAreEmpty() {
    var customAttributes = withCoverage(new Coverage()
      .withBeginCoverage("")
      .withEndCoverage(""));
    assertDoesNotThrow(() -> validator.validate(customAttributes));
  }

  private PackageCustomAttributes withDescription(String description) {
    return new PackageCustomAttributes(description, null, null, null, null);
  }

  private PackageCustomAttributes withDisplayName(String displayName) {
    return new PackageCustomAttributes(null, displayName, null, null, null);
  }

  private PackageCustomAttributes withAltNames(List<PackageAltName> altNames) {
    return new PackageCustomAttributes(null, null, null, altNames, null);
  }

  private PackageCustomAttributes withUrl(String url) {
    return new PackageCustomAttributes(null, null, url, null, null);
  }

  private PackageCustomAttributes withCoverage(Coverage coverage) {
    return new PackageCustomAttributes(null, null, null, null, coverage);
  }
}
