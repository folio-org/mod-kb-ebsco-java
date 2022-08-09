package org.folio.client.uc;

import io.vertx.core.json.JsonObject;

public class UcFailedRequestException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int statusCode;
  private final String errorMessage;

  public UcFailedRequestException(int statusCode, JsonObject responseBody) {
    super("Failed APIGEE request with status: " + statusCode);
    this.statusCode = statusCode;
    if (responseBody.containsKey("fault")) {
      this.errorMessage = responseBody.getJsonObject("fault").getString("faultstring");
    } else {
      errorMessage = responseBody.toString();
    }
  }

  public UcFailedRequestException(int statusCode, String message) {
    super("Failed APIGEE request with status: " + statusCode);
    this.statusCode = statusCode;
    this.errorMessage = message;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
