package org.folio.rest.validator.packages;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePostData;
import org.folio.rest.jaxrs.model.PackagePostDataAttributes;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PackagePostValidatorTest {

  @Mock
  private PackageCustomAttributesValidator customAttributesValidator;

  @InjectMocks
  private PackagesPostBodyValidator validator;

  @BeforeEach
  void setUp() {
    lenient().doNothing().when(customAttributesValidator).validate(any());
  }

  @Test
  void shouldThrowExceptionWhenNoPostBody() {
    assertThrows(InputValidationException.class, () -> validator.validate(null));
  }

  @Test
  void shouldThrowExceptionWhenEmptyBody() {
    var postRequest = new PackagePostRequest();
    assertThrows(InputValidationException.class, () -> validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenNoPostData() {
    var postRequest = new PackagePostRequest().withData(null);
    assertThrows(InputValidationException.class, () -> validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenEmptyPostData() {
    var postRequest = new PackagePostRequest().withData(new PackagePostData());
    assertThrows(InputValidationException.class, () -> validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenEmptyPostDataAttributes() {
    var postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()));
    assertThrows(InputValidationException.class, () -> validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenPostDataAttributesNameIsEmpty() {
    var postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("")));
    assertThrows(InputValidationException.class, () -> validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenPostDataAttributesTypeIsNull() {
    var postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("name")));
    assertThrows(InputValidationException.class, () -> validator.validate(postRequest));
  }

  @Test
  void shouldThrowExceptionWhenBeginCoverageIsNull() {
    var postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("name")
          .withContentType(ContentType.STREAMING_MEDIA)
          .withCustomCoverage(new Coverage())));
    assertThrows(InputValidationException.class, () -> validator.validate(postRequest));
  }

  @Test
  void shouldNotThrowExceptionWhenCoverageIsNull() {
    var postRequest = new PackagePostRequest()
      .withData(new PackagePostData()
        .withAttributes(new PackagePostDataAttributes()
          .withName("name")
          .withContentType(ContentType.STREAMING_MEDIA)));
    assertDoesNotThrow(() -> validator.validate(postRequest));
  }
}
