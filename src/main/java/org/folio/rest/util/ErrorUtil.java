package org.folio.rest.util;

import static java.util.Collections.singletonList;
import static org.folio.common.ListUtils.mapItems;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import org.folio.holdingsiq.model.Error;
import org.folio.holdingsiq.model.Errors;
import org.folio.holdingsiq.service.exception.ServiceResponseException;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.JsonapiErrorResponse;

/**
 * Util class for creating errors.
 */
public final class ErrorUtil {
  private ErrorUtil() {
  }

  public static JsonapiError createError(String errorMessage) {
    return createError(errorMessage, null);
  }

  public static JsonapiError createError(String errorMessage, String errorMessageDetails) {
    JsonapiError error = new JsonapiError();
    JsonapiErrorResponse errorResponse = createErrorResponse(errorMessage, errorMessageDetails);
    error.setErrors(singletonList(errorResponse));
    error.setJsonapi(RestConstants.JSONAPI);
    return error;
  }

  public static JsonapiError createErrorFromRmApiResponse(ServiceResponseException rmApiException) {
    try {
      var mapper = JsonMapper.builder().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true).build();

      Errors errors = mapper.readValue(rmApiException.getResponseBody(), Errors.class);
      JsonapiError configurationError = new JsonapiError();
      Errors errorsObject = errors;

      if (errorsObject.getErrorList() == null) {
        errorsObject = errorsObject.toBuilder()
          .errorList(singletonList(mapper.readValue(rmApiException.getResponseBody(), Error.class)))
          .build();
      }

      List<JsonapiErrorResponse> jsonApiErrors = mapItems(errorsObject.getErrorList(),
        error -> new JsonapiErrorResponse()
          .withTitle(error.getMessage()));
      configurationError.setErrors(jsonApiErrors);
      configurationError.setJsonapi(RestConstants.JSONAPI);
      return configurationError;
    } catch (Exception e) {
      //If RM API didn't return valid json then just include response body as error message
      return createError(rmApiException.getMessage());
    }
  }

  private static JsonapiErrorResponse createErrorResponse(String errorMessage, String errorMessageDetails) {
    JsonapiErrorResponse errorResponse = new JsonapiErrorResponse();
    errorResponse.setTitle(errorMessage);
    if (errorMessageDetails != null) {
      errorResponse.setDetail(errorMessageDetails);
    }
    return errorResponse;
  }

}
