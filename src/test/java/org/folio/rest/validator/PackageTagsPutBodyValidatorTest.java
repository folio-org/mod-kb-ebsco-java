package org.folio.rest.validator;

import org.junit.Test;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ContentType;
import org.folio.rest.jaxrs.model.PackageTagsDataAttributes;
import org.folio.rest.jaxrs.model.PackageTagsPutData;
import org.folio.rest.jaxrs.model.PackageTagsPutRequest;

public class PackageTagsPutBodyValidatorTest {
  private PackageTagsPutBodyValidator validator = new PackageTagsPutBodyValidator();

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNameIsEmpty(){
    PackageTagsPutRequest request = new PackageTagsPutRequest()
      .withData(new PackageTagsPutData()
        .withAttributes(new PackageTagsDataAttributes()
          .withName("")
          .withContentType(ContentType.E_BOOK)
        ));
    validator.validate(request);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenContentTypeIsNull(){
    PackageTagsPutRequest request = new PackageTagsPutRequest()
      .withData(new PackageTagsPutData()
        .withAttributes(new PackageTagsDataAttributes()
          .withName("name")
        ));
    validator.validate(request);
  }

}
