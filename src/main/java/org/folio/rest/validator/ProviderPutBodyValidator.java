package org.folio.rest.validator;

import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ProviderPutRequest;

public class ProviderPutBodyValidator {

  private static final int MAX_TOKEN_LENGTH = 500;
  private static final String INVALID_VALUE = "Invalid value";
  private static final String VALUE_TOO_LONG = "Value is too long (maximum is 500 characters)";

  /**
   * @throws InputValidationException
   *           if put validation fails
   */
  public void validate(ProviderPutRequest putRequest) {

    if (putRequest != null && putRequest.getData() != null && putRequest.getData().getAttributes() != null
        && putRequest.getData().getAttributes().getProviderToken() != null
        && putRequest.getData().getAttributes().getProviderToken().getValue() != null) {
      if (putRequest.getData().getAttributes().getProviderToken().getValue().length() > MAX_TOKEN_LENGTH) {
        throw new InputValidationException(INVALID_VALUE, VALUE_TOO_LONG);
      }
    }
  }
}

