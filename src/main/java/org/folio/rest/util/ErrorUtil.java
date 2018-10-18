package org.folio.rest.util;

import org.folio.rest.jaxrs.model.ConfigurationUnprocessableError;
import org.folio.rest.jaxrs.model.Error;

import java.util.Collections;

/**
 * Util class for creating errors
 */
public class ErrorUtil {
  private ErrorUtil() { }

  public static ConfigurationUnprocessableError createError(String errorMessage) {
    ConfigurationUnprocessableError configurationError = new ConfigurationUnprocessableError();
    Error error = new Error();
    error.setDetail(errorMessage);
    configurationError.setErrors(Collections.singletonList(error));
    return configurationError;
  }
}
