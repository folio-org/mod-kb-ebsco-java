package org.folio.rest.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;
import org.folio.holdingsiq.service.exception.ServiceResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ErrorUtilTest {

  private static ServiceResponseException exception(String responseBody) {
    return new ServiceResponseException(
      "Invalid RMAPI response Code = 400 Message = Bad Request Body = " + responseBody,
      400, "Bad Request", responseBody, "https://example.com/packages");
  }

  @ParameterizedTest
  @MethodSource("responseBodiesWithExpectedTitle")
  void createErrorFromRmApiResponse_extractsCleanTitle(String body) {
    var result = ErrorUtil.createErrorFromRmApiResponse(exception(body));

    assertEquals(1, result.getErrors().size());
    assertEquals("Custom Package with the provided name already exists", result.getErrors().getFirst().getTitle());
  }

  @Test
  void createErrorFromRmApiResponse_fallsBackToRawBodyWhenJsonParsingFails() {
    var body = "upstream service unavailable";
    var result = ErrorUtil.createErrorFromRmApiResponse(exception(body));

    assertEquals(1, result.getErrors().size());
    assertEquals("upstream service unavailable", result.getErrors().getFirst().getTitle());
  }

  @Test
  void createErrorFromRmApiResponse_fallsBackToExceptionMessageWhenBodyIsEmpty() {
    var exception = new ServiceResponseException(
      "Requested resource http://example.com/packages/123 not found",
      404, "Not Found", "", "http://example.com/packages/123");
    var result = ErrorUtil.createErrorFromRmApiResponse(exception);

    assertEquals(1, result.getErrors().size());
    assertEquals("Requested resource http://example.com/packages/123 not found",
      result.getErrors().getFirst().getTitle());
  }

  @Test
  void extractMessageFromBody_extractsMessageWithCodePrefix() {
    assertEquals("Custom Package with the provided name already exists",
      ErrorUtil.extractMessageFromBody("{message:1009: Custom Package with the provided name already exists }"));
  }

  @Test
  void extractMessageFromBody_extractsMessageFromVtxErrorString() {
    assertEquals("Custom Package with the provided name already exists",
      ErrorUtil.extractMessageFromBody(
        "400 Bad Request on POST request for https://example.com/packages: "
          + "{message:1009: Custom Package with the provided name already exists }"));
  }

  @Test
  void extractMessageFromBody_extractsMessageWithoutCodePrefix() {
    assertEquals("Custom Package with the provided name already exists",
      ErrorUtil.extractMessageFromBody("{message:Custom Package with the provided name already exists}"));
  }

  @Test
  void extractMessageFromBody_returnsBodyWhenPatternDoesNotMatch() {
    assertEquals("some raw error", ErrorUtil.extractMessageFromBody("some raw error"));
  }

  @Test
  void extractMessageFromBody_returnsNullForNullInput() {
    assertNull(ErrorUtil.extractMessageFromBody(null));
  }

  private static Stream<String> responseBodiesWithExpectedTitle() {
    // Production format: upstream proxy wraps internal error in a JSON errors array where
    // the message field contains a full Vert.x HttpStatusException string with non-JSON body.
    var productionFormat =
      """
      {"errors":[{"code":"0","subCode":"0","message":"400 Bad Request on POST request for \
      https://example.com/packages: {message:1009: Custom Package with the provided name already exists }"}]}
      """;
    var cleanMessage =
      """
      {"errors":[{"code":1009,"subCode":0,"message":"Custom Package with the provided name already exists"}]}
      """;
    var messageWithCodePrefix =
      """
      {"errors":[{"code":1009,"subCode":0,"message":"1009: Custom Package with the provided name already exists"}]}
      """;
    return Stream.of(productionFormat, cleanMessage, messageWithCodePrefix);
  }
}
