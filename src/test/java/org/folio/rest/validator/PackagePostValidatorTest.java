package org.folio.rest.validator;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePostData;
import org.folio.rest.jaxrs.model.PackagePostDataAttributes;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.junit.jupiter.api.Test;

class PackagePostValidatorTest {

  private final PackagesPostBodyValidator validator = new PackagesPostBodyValidator();

  @Test
  void shouldThrowExceptionWhenNoPostBody() {
    PackagePostRequest postRequest = null;
    assertThrows(InputValidationException.class, () ->
      validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenEmptyBody() {
    PackagePostRequest postRequest = new PackagePostRequest();
    assertThrows(InputValidationException.class, () ->
      validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenNoPostData() {
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(null);
    assertThrows(InputValidationException.class, () ->
      validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenEmptyPostData() {
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData());
    assertThrows(InputValidationException.class, () ->
      validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenEmptyPostDataAttributes() {
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()));
    assertThrows(InputValidationException.class, () ->
      validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenPostDataAttributesNameIsEmpty() {
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("")));
    assertThrows(InputValidationException.class, () ->
      validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenPostDataAttributesTypeIsNull() {
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("name")));
    assertThrows(InputValidationException.class, () ->
      validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenPostDataAttributeCustomCoverageBeginDateIsNull() {
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("name").withContentType(ContentType.STREAMING_MEDIA)
          .withCustomCoverage(new Coverage())));
    assertThrows(InputValidationException.class, () ->
      validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenPostDataAttributeCustomCoverageWithEmptyBeginDate() {
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.STREAMING_MEDIA)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")
          .withEndCoverage("2003-11-01"))
      ));
    assertThrows(InputValidationException.class, () ->
      validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenPostDataAttributeCustomCoverageBeginDateIsInvalidFormat() {
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("name")
          .withContentType(ContentType.STREAMING_MEDIA)
          .withCustomCoverage(new Coverage()
            .withBeginCoverage("-01"))));
    assertThrows(InputValidationException.class, () ->
      validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenPostDataAttributeCustomCoverageEndDateIsInvalidFormat() {
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("name")
          .withContentType(ContentType.STREAMING_MEDIA)
          .withCustomCoverage(new Coverage()
            .withBeginCoverage("2003-11-01")
            .withEndCoverage("-01"))));
    assertThrows(InputValidationException.class, () ->
      validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenPostDataAttributeCustomCoverageBeginDateIsBeforeEndDate() {
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.STREAMING_MEDIA)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2003-12-01")
          .withEndCoverage("2003-11-01"))
      ));
    assertThrows(InputValidationException.class, () ->
      validator.validate(postRequest));
  }

  @Test
  void shouldNotThrowExceptionWhenPostDataAttributeCustomCoverageIsNull() {
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.STREAMING_MEDIA)
      ));
    validator.validate(postRequest);
  }

  @Test
  void shouldNotThrowExceptionWhenPostDataAttributeCustomCoverageWithValidDates() {
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.STREAMING_MEDIA)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2003-11-01")
          .withEndCoverage("2003-12-01"))
      ));
    validator.validate(postRequest);
  }

  @Test
  void shouldNotThrowExceptionWhenPostDataAttributeCustomCoverageWithEmptyDates() {
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.STREAMING_MEDIA)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")
          .withEndCoverage(""))
      ));
    validator.validate(postRequest);
  }

  @Test
  void shouldNotThrowExceptionWhenPostDataAttributeCustomCoverageBeginDateIsEmpty() {
    PackagePostRequest postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("name")
          .withContentType(ContentType.STREAMING_MEDIA)
          .withCustomCoverage(new Coverage()
            .withBeginCoverage(""))));
    validator.validate(postRequest);
  }

  @Test
  void shouldNotThrowExceptionWhenPostDataAttributeCustomCoverageWithValidBeginDateAndEndDateIsEmpty() {
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.STREAMING_MEDIA)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2003-11-01")
          .withEndCoverage(""))
      ));
    validator.validate(postRequest);
  }

  @Test
  void shouldNotThrowExceptionWhenPostDataAttributeCustomCoverageWithValidBeginDateAndEndDateIsNull() {
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.STREAMING_MEDIA)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("2003-11-01")
          .withEndCoverage(null))
      ));
    validator.validate(postRequest);
  }

  @Test
  void shouldNotThrowExceptionWhenPostDataAttributeCustomCoverageWithValidBeginDateAndEndDateIsNullw() {
    PackagePostRequest postRequest = new PackagePostRequest().withData(new PackagePostData()
      .withAttributes(new PackagePostDataAttributes()
        .withName("name")
        .withContentType(ContentType.STREAMING_MEDIA)
        .withCustomCoverage(new Coverage()
          .withBeginCoverage("")
          .withEndCoverage(null))
      ));
    validator.validate(postRequest);
  }
}
