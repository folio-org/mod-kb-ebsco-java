package org.folio.rest.validator;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.model.AccessTypeDataAttributes;

public class AccessTypesBodyValidatorTest {
  @Rule
  public ExpectedException expectedEx = ExpectedException.none();
  private AccessTypesBodyValidator validator = new AccessTypesBodyValidator();

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenNoPutBody() {
    AccessTypeCollectionItem postRequest = null;
    validator.validate(postRequest, null);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenEmptyPostDataAttributes() {
    AccessTypeCollectionItem postRequest = new AccessTypeCollectionItem();
    postRequest.withAttributes(new AccessTypeDataAttributes());
    validator.validate(postRequest, null);
  }

  @Test
  public void shouldThrowExceptionWhenNameIsTooLong() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid name");
    final AccessTypeCollectionItem request = new AccessTypeCollectionItem()
      .withAttributes(
          new AccessTypeDataAttributes().withName(RandomStringUtils.randomAlphanumeric(76)));
    validator.validate(request, null);
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
    validator.validate(request, null);
  }

  @Test
  public void shouldNotThrowExceptionWhenDescriptionIsNull() {
    final AccessTypeCollectionItem request = new AccessTypeCollectionItem()
      .withAttributes(
        new AccessTypeDataAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(75))
          .withDescription(null));
    validator.validate(request, null);
  }

  @Test
  public void shouldThrowExceptionWhenInvalidId() {
    expectedEx.expect(InputValidationException.class);
    expectedEx.expectMessage("Invalid id");
    final AccessTypeCollectionItem request = new AccessTypeCollectionItem()
      .withAttributes(
        new AccessTypeDataAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(75))
          .withDescription(RandomStringUtils.randomAlphanumeric(150)));
    validator.validate(request, "c0af6d39-6705-43d7-b91e-c01c3549ddww");
  }

  @Test
  public void shouldNotThrowExceptionWhenValidParameters() {
    final AccessTypeCollectionItem request = new AccessTypeCollectionItem()
      .withAttributes(
        new AccessTypeDataAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(75))
          .withDescription(RandomStringUtils.randomAlphanumeric(150)));
    validator.validate(request, null);
  }
}
