package org.folio.rest.validator;

import org.apache.commons.lang.RandomStringUtils;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AccessTypesBodyValidatorTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  private final AccessTypesBodyValidator validator = new AccessTypesBodyValidator(75, 150);

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNoPutBody() {
    validator.validate(null, null);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenEmptyPostDataAttributes() {
    final AccessType request = stubAccessType();
    request.setAttributes(new AccessTypeDataAttributes());
    validator.validate(null, request);
  }

  @Test
  public void shouldThrowExceptionWhenNameIsTooLong() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid name");
    final AccessType request = stubAccessType();
    request.getAttributes().setName(RandomStringUtils.randomAlphanumeric(76));
    validator.validate(null, request);
  }

  @Test
  public void shouldThrowExceptionWhenDescriptionIsTooLong() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid description");
    final AccessType request = stubAccessType();
    request.getAttributes().setDescription(RandomStringUtils.randomAlphanumeric(151));
    validator.validate(null, request);
  }

  @Test
  public void shouldThrowExceptionWhenCredentialsIdNotEquals() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid credentialsId");
    final AccessType request = stubAccessType();
    request.getAttributes().setCredentialsId("10");
    validator.validate("1", request);
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
      new AccessTypeDataAttributes().withName(RandomStringUtils.randomAlphanumeric(75))
        .withDescription(RandomStringUtils.randomAlphanumeric(150)));
  }
}
