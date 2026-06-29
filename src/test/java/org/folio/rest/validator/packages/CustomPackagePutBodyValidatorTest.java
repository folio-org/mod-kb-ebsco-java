package org.folio.rest.validator.packages;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.util.PackagesTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomPackagePutBodyValidatorTest {

  @Mock
  private PackageCustomAttributesValidator customAttributesValidator;

  @InjectMocks
  private CustomPackagePutBodyValidator validator;

  @BeforeEach
  void setUp() {
    lenient().doNothing().when(customAttributesValidator).validate(any());
  }

  @Test
  void shouldThrowExceptionOnEmptyName() {
    var request = PackagesTestUtil.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withContentType(ContentType.MIXED_CONTENT)
        .withName(""));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertTrue(exception.getMessage().contains("name"));
  }

  @Test
  void shouldThrowExceptionOnNullContentType() {
    var request = PackagesTestUtil.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withName("package name"));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(request));
    assertTrue(exception.getMessage().contains("contentType"));
  }

  @Test
  void shouldNotThrowExceptionForValidRequest() {
    var request = PackagesTestUtil.getPackagePutRequest(
      new PackagePutDataAttributes()
        .withName("package name")
        .withContentType(ContentType.MIXED_CONTENT));
    assertDoesNotThrow(() -> validator.validate(request));
  }
}
