package org.folio.rest.util;


import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.JsonapiErrorResponse;
import org.folio.rmapi.exception.RMAPIServiceException;
import org.folio.rmapi.model.Errors;

/**
 * Util class for creating errors
 */
public class ErrorUtil {
  private ErrorUtil() {
  }

  public static JsonapiError createError(String errorMessage) {
    return createError(errorMessage, null);
  }

  public static JsonapiError createError(String errorMessage, String errorMessageDetails) {
    JsonapiError configurationError = new JsonapiError();
    JsonapiErrorResponse error = new JsonapiErrorResponse();
    error.setTitle(errorMessage);
    if (errorMessageDetails != null) {
      error.setDetail(errorMessageDetails);
    }
    configurationError.setErrors(Collections.singletonList(error));
    configurationError.setJsonapi(RestConstants.JSONAPI);
    return configurationError;
  }
  
  public static JsonapiError createErrorFromRMAPIResponse(RMAPIServiceException rmApiException) {
    try {
      final JsonObject instanceJSON = new JsonObject(rmApiException.getResponseBody());
      Errors errors = instanceJSON.mapTo(Errors.class);
      JsonapiError configurationError = new JsonapiError();
      List<JsonapiErrorResponse> jsonApiErrors = errors.getErrorList().stream()
        .map(error -> new JsonapiErrorResponse()
          .withTitle(error.getMessage()))
        .collect(Collectors.toList());
      configurationError.setErrors(jsonApiErrors);
      configurationError.setJsonapi(RestConstants.JSONAPI);
      return configurationError;
    }
    catch(DecodeException e){
      //If RM API didn't return valid json then just include response body as error message
      return createError(rmApiException.getMessage());
    }
  }
}
