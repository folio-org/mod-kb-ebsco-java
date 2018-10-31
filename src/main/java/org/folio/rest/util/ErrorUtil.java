package org.folio.rest.util;


import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.JsonapiErrorResponse;

import java.util.Collections;

/**
 * Util class for creating errors
 */
public class ErrorUtil {
  private ErrorUtil() {
  }

  public static JsonapiError createError(String errorMessage) {
    JsonapiError configurationError = new JsonapiError();
    JsonapiErrorResponse error = new JsonapiErrorResponse();
    error.setDetail(errorMessage);
    configurationError.setErrors(Collections.singletonList(error));
    configurationError.setJsonapi(RestConstants.JSONAPI);
    return configurationError;
  }
}
