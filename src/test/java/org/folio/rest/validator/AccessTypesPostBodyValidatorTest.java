package org.folio.rest.validator;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;

public class AccessTypesPostBodyValidatorTest {
  @Rule
  public ExpectedException expectedEx = ExpectedException.none();
  private AccessTypePostBodyValidator validator = new AccessTypePostBodyValidator();

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNoPutBody() {
    AccessTypeCollectionItem postRequest = null;
    validator.validate(postRequest);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenEmptyPostDataAttributes() {
    AccessTypeCollectionItem postRequest = new AccessTypeCollectionItem();
    postRequest.withAttributes(new AccessTypeDataAttributes());
    validator.validate(postRequest);
  }

  @Test
  public void shouldThrowExceptionWhenNameIsTooLong() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid name");
    final AccessTypeCollectionItem request = new AccessTypeCollectionItem()
      .withAttributes(
          new AccessTypeDataAttributes().withName(RandomStringUtils.randomAlphanumeric(76)));
    validator.validate(request);
  }

  @Test
  public void shouldThrowExceptionWhenDescriptionIsTooLong() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid description");
    final AccessTypeCollectionItem request = new AccessTypeCollectionItem()
      .withAttributes(
        new AccessTypeDataAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(75))
          .withDescription(RandomStringUtils.randomAlphanumeric(151)));
    validator.validate(request);
  }

  @Test
  public void shouldNotThrowExceptionWhenDescriptionIsNull() {
    final AccessTypeCollectionItem request = new AccessTypeCollectionItem()
      .withAttributes(
        new AccessTypeDataAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(75))
          .withDescription(null));
    validator.validate(request);
  }

  @Test
  public void shouldNotThrowExceptionWhenValidParameters() {
    final AccessTypeCollectionItem request = new AccessTypeCollectionItem()
      .withAttributes(
        new AccessTypeDataAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(75))
          .withDescription(RandomStringUtils.randomAlphanumeric(150)));
    validator.validate(request);
  }
}
