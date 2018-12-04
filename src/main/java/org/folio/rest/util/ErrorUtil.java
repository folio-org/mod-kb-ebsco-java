package org.folio.rest.util;


import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.JsonapiErrorResponse;
import org.folio.rmapi.exception.RMAPIServiceException;
import org.folio.rmapi.model.Errors;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

      Errors errors = objectMapper.readValue(rmApiException.getResponseBody(), Errors.class);
      JsonapiError configurationError = new JsonapiError();
      Errors errorsObject = errors;

      if (errorsObject.getErrorList() == null) {
        errorsObject = errorsObject.toBuilder()
          .errorList(Collections.singletonList(objectMapper.readValue(rmApiException.getResponseBody(), org.folio.rmapi.model.Error.class)))
          .build();
      }

      List<JsonapiErrorResponse> jsonApiErrors = errorsObject.getErrorList().stream()
        .map(error -> new JsonapiErrorResponse()
          .withTitle(error.getMessage()))
        .collect(Collectors.toList());
      configurationError.setErrors(jsonApiErrors);
      configurationError.setJsonapi(RestConstants.JSONAPI);
      return configurationError;
    }
    catch(Exception e){
      //If RM API didn't return valid json then just include response body as error message
      return createError(rmApiException.getMessage());
    }
  }
}
