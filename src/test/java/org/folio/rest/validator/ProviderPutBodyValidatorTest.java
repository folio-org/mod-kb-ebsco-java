package org.folio.rest.validator;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.lang3.RandomStringUtils;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ProviderPutData;
import org.folio.rest.jaxrs.model.ProviderPutDataAttributes;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.Token;
import org.junit.jupiter.api.Test;

class ProviderPutBodyValidatorTest {

  private final ProviderPutBodyValidator validator = new ProviderPutBodyValidator();

  @Test
  void shouldNotThrowExceptionWhenTokenIsMissing() {
    ProviderPutRequest request = new ProviderPutRequest();
    validator.validate(request);
  }

  @Test
  void shouldNotThrowExceptionWhenTokenIsValidLength() {
    Token providerToken = new Token();
    providerToken.setValue(RandomStringUtils.insecure().nextAlphanumeric(500));

    ProviderPutRequest request = new ProviderPutRequest()
      .withData(new ProviderPutData().withAttributes(new ProviderPutDataAttributes().withProviderToken(providerToken)));
    validator.validate(request);
  }

  @Test
  void shouldThrowExceptionWhenTokenIsTooLong() {
    Token providerToken = new Token();
    providerToken.setValue(RandomStringUtils.insecure().nextAlphanumeric(501));
    ProviderPutRequest request = new ProviderPutRequest()
      .withData(new ProviderPutData().withAttributes(new ProviderPutDataAttributes().withProviderToken(providerToken)));
    assertThrows(InputValidationException.class, () ->
      validator.validate(request));
  }
}
