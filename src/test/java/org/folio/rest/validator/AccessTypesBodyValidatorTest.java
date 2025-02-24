package org.folio.rest.validator;

import static org.apache.commons.lang3.RandomStringUtils.insecure;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;
import org.junit.Test;

public class AccessTypesBodyValidatorTest {

  private final AccessTypesBodyValidator validator = new AccessTypesBodyValidator(75, 150);

  @Test
  public void shouldThrowExceptionWhenNoPutBody() {
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(null, null));
    assertEquals("Invalid request body", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenEmptyPostDataAttributes() {
    final AccessType request = stubAccessType();
    request.setAttributes(new AccessTypeDataAttributes());
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(null, request));
    assertEquals("Invalid name", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenNameIsTooLong() {
    final AccessType request = stubAccessType();
    request.getAttributes().setName(insecure().nextAlphanumeric(76));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(null, request));
    assertEquals("Invalid name", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenDescriptionIsTooLong() {
    final AccessType request = stubAccessType();
    request.getAttributes().setDescription(insecure().nextAlphanumeric(151));
    var exception = assertThrows(InputValidationException.class, () -> validator.validate(null, request));
    assertEquals("Invalid description", exception.getMessage());
  }

  @Test
  public void shouldThrowExceptionWhenCredentialsIdNotEquals() {
    final AccessType request = stubAccessType();
    request.getAttributes().setCredentialsId("10");
    var exception = assertThrows(InputValidationException.class, () -> validator.validate("1", request));
    assertEquals("Invalid credentialsId", exception.getMessage());
  }

  @Test
  public void shouldNotThrowExceptionWhenDescriptionIsNull() {
    final AccessType request = stubAccessType();
    request.getAttributes().setDescription(null);
    validator.validate(null, request);
  }

  @Test
  public void shouldNotThrowExceptionWhenValidParameters() {
    final AccessType request = stubAccessType();
    validator.validate(null, request);
  }

  private AccessType stubAccessType() {
    return new AccessType().withAttributes(
      new AccessTypeDataAttributes().withName(insecure().nextAlphanumeric(75))
        .withDescription(insecure().nextAlphanumeric(150)));
  }
}
