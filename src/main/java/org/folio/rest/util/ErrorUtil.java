package org.folio.rest.util;


import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.JsonapiErrorResponse;
import org.folio.rmapi.exception.RMAPIServiceException;
import org.folio.rmapi.model.Errors;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;

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
    JsonapiError error = new JsonapiError();
    JsonapiErrorResponse errorResponse = new JsonapiErrorResponse();
    errorResponse.setTitle(errorMessage);
    if (errorMessageDetails != null) {
      errorResponse.setDetail(errorMessageDetails);
    }
    error.setErrors(Collections.singletonList(errorResponse));
    error.setJsonapi(RestConstants.JSONAPI);
    return error;
  }

  public static JsonapiError createErrorFromRMAPIResponse(RMAPIServiceException rmApiException) {
    try {
      final JsonObject instanceJSON = new JsonObject(rmApiException.getResponseBody());
      Errors errors = instanceJSON.mapTo(Errors.class);
      JsonapiError configurationError = new JsonapiError();
      Errors errorsObject = errors;

      if (errorsObject.getErrorList() == null) {
        errorsObject = errorsObject.toBuilder()
          .errorList(Collections.singletonList(instanceJSON.mapTo(org.folio.rmapi.model.Error.class)))
          .build();
      }

      List<JsonapiErrorResponse> jsonApiErrors = errorsObject.getErrorList().stream()
        .map(error -> new JsonapiErrorResponse()
          .withTitle(error.getMessage()))
        .collect(Collectors.toList());
      configurationError.setErrors(jsonApiErrors);
      configurationError.setJsonapi(RestConstants.JSONAPI);
      return configurationError;
    } catch (DecodeException e) {
      //If RM API didn't return valid json then just include response body as error message
      return createError(rmApiException.getMessage());
    }
  }
}
