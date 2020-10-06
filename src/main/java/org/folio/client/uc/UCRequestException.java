package org.folio.client.uc;

import io.vertx.core.json.JsonObject;

public class UCRequestException extends RuntimeException {

  private int statusCode;
  private JsonObject responseBody;
  private String errorMessage;

  public UCRequestException(int statusCode, JsonObject responseBody) {
    super("Failed APIGEE request with status: " + statusCode);
    this.statusCode = statusCode;
    this.responseBody = responseBody;
  }

  public UCRequestException(int statusCode, String message) {
    super("Failed APIGEE request with status: " + statusCode);
    this.statusCode = statusCode;
    this.errorMessage = message;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public JsonObject getResponseBody() {
    return responseBody;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
