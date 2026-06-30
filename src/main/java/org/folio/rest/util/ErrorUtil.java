package org.folio.rest.util;

import static java.util.Collections.singletonList;
import static org.folio.common.ListUtils.mapItems;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.regex.Pattern;
import org.folio.holdingsiq.model.Error;
import org.folio.holdingsiq.model.Errors;
import org.folio.holdingsiq.service.exception.ServiceResponseException;
import org.folio.rest.jaxrs.model.JsonapiError;
import org.folio.rest.jaxrs.model.JsonapiErrorResponse;

/**
 * Util class for creating errors.
 */
public final class ErrorUtil {

  private static final Pattern MESSAGE_CODE_PATTERN = Pattern.compile("^\\d+:\\s*");
  @SuppressWarnings("java:S8786")
  private static final Pattern NON_JSON_MESSAGE_PATTERN =
    Pattern.compile("\\{\\s*message\\s*:\\s*(?:\\d+:\\s*)?([^}]*+)}\\s*$");

  private ErrorUtil() {
  }

  public static JsonapiError createErrors(List<String> errorMessage) {
    var jsonapiErrorResponses = errorMessage.stream()
      .map(msg -> createErrorResponse(msg, null))
      .toList();
    return new JsonapiError()
      .withErrors(jsonapiErrorResponses)
      .withJsonapi(RestConstants.JSONAPI);
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
          .withTitle(stripMessageCode(extractMessageFromBody(error.getMessage()))));
      configurationError.setErrors(jsonApiErrors);
      configurationError.setJsonapi(RestConstants.JSONAPI);
      return configurationError;
    } catch (Exception e) {
      var responseBody = rmApiException.getResponseBody();
      return createError(responseBody != null && !responseBody.isBlank()
        ? extractMessageFromBody(responseBody)
        : rmApiException.getMessage());
    }
  }

  private static String stripMessageCode(String message) {
    if (message == null) {
      return null;
    }
    return MESSAGE_CODE_PATTERN.matcher(message).replaceFirst("");
  }

  static String extractMessageFromBody(String responseBody) {
    if (responseBody == null) {
      return null;
    }
    var matcher = NON_JSON_MESSAGE_PATTERN.matcher(responseBody);
    return matcher.find() ? matcher.group(1).strip() : responseBody;
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
