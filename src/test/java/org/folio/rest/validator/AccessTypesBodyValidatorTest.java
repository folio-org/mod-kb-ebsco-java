package org.folio.rest.validator;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;

public class AccessTypesBodyValidatorTest {

  private final AccessTypesBodyValidator validator = new AccessTypesBodyValidator(75, 150);

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNoPutBody() {
    AccessType postRequest = null;
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenEmptyPostDataAttributes() {
    AccessType postRequest = new AccessType();
    postRequest.withAttributes(new AccessTypeDataAttributes());
    validator.validate(postRequest);
  }

  @Test
  public void shouldThrowExceptionWhenNameIsTooLong() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid name");
    final AccessType request = new AccessType()
      .withAttributes(
        new AccessTypeDataAttributes().withName(RandomStringUtils.randomAlphanumeric(76)));
    validator.validate(request);
  }

  @Test
  public void shouldThrowExceptionWhenDescriptionIsTooLong() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid description");
    final AccessType request = new AccessType()
      .withAttributes(
        new AccessTypeDataAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(75))
          .withDescription(RandomStringUtils.randomAlphanumeric(151)));
    validator.validate(request);
  }

  @Test
  public void shouldNotThrowExceptionWhenDescriptionIsNull() {
    final AccessType request = new AccessType()
      .withAttributes(
        new AccessTypeDataAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(75))
          .withDescription(null));
    validator.validate(request);
  }


  @Test
  public void shouldNotThrowExceptionWhenValidParameters() {
    final AccessType request = new AccessType()
      .withAttributes(
        new AccessTypeDataAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(75))
          .withDescription(RandomStringUtils.randomAlphanumeric(150)));
    validator.validate(request);
  }
}
