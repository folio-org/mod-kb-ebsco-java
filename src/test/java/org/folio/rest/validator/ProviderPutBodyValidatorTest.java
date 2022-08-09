package org.folio.rest.validator;

import org.apache.commons.lang.RandomStringUtils;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ProviderPutData;
import org.folio.rest.jaxrs.model.ProviderPutDataAttributes;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.Token;
import org.junit.Test;

public class ProviderPutBodyValidatorTest {

  private final ProviderPutBodyValidator validator = new ProviderPutBodyValidator();

  @Test
  public void shouldNotThrowExceptionWhenTokenIsMissing() {
    ProviderPutRequest request = new ProviderPutRequest();
    validator.validate(request);
  }

  @Test
  public void shouldNotThrowExceptionWhenTokenIsValidLength() {
    Token providerToken = new Token();
    providerToken.setValue(RandomStringUtils.randomAlphanumeric(500));

    ProviderPutRequest request = new ProviderPutRequest()
      .withData(new ProviderPutData().withAttributes(new ProviderPutDataAttributes().withProviderToken(providerToken)));
    validator.validate(request);
  }

  @Test(expected = InputValidationException.class)
  public void shouldThrowExceptionWhenTokenIsTooLong() {
    Token providerToken = new Token();
    providerToken.setValue(RandomStringUtils.randomAlphanumeric(501));

    ProviderPutRequest request = new ProviderPutRequest()
      .withData(new ProviderPutData().withAttributes(new ProviderPutDataAttributes().withProviderToken(providerToken)));
    validator.validate(request);
  }

}
