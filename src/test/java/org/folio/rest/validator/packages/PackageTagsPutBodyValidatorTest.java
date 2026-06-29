package org.folio.rest.validator.packages;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.PackageTagsDataAttributes;
import org.folio.rest.jaxrs.model.PackageTagsPutData;
import org.folio.rest.jaxrs.model.PackageTagsPutRequest;
import org.junit.jupiter.api.Test;

class PackageTagsPutBodyValidatorTest {
  private final PackageTagsPutBodyValidator validator = new PackageTagsPutBodyValidator();

  @Test
  void shouldThrowExceptionWhenNameIsEmpty() {
    PackageTagsPutRequest request = new PackageTagsPutRequest()
      .withData(new PackageTagsPutData()
        .withAttributes(new PackageTagsDataAttributes()
          .withName("")
          .withContentType(ContentType.E_BOOK)
        ));
    assertThrows(InputValidationException.class, () -> validator.validate(request));
  }

  @Test
  void shouldThrowExceptionWhenContentTypeIsNull() {
    PackageTagsPutRequest request = new PackageTagsPutRequest()
      .withData(new PackageTagsPutData()
        .withAttributes(new PackageTagsDataAttributes()
          .withName("name")
        ));
    assertThrows(InputValidationException.class, () -> validator.validate(request));
  }
}
