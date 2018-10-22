package org.folio.rest.util;

import org.folio.rest.jaxrs.model.ConfigurationUnprocessableError;
import org.folio.rest.jaxrs.model.ConfigurationUnprocessableErrorResponse;

import java.util.Collections;

/**
 * Util class for creating errors
 */
public class ErrorUtil {
  private ErrorUtil() {
  }

  public static ConfigurationUnprocessableError createError(String errorMessage) {
    ConfigurationUnprocessableError configurationError = new ConfigurationUnprocessableError();
    ConfigurationUnprocessableErrorResponse error = new ConfigurationUnprocessableErrorResponse();
    error.setDetail(errorMessage);
    configurationError.setErrors(Collections.singletonList(error));
    configurationError.setJsonapi(RestConstants.JSONAPI);
    return configurationError;
  }
}
