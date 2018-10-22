package org.folio.rmapi.exception;

public class RMAPIServiceException extends RMAPIException {

  private static final long serialVersionUID = 1L;

  private final Integer rmapiCode;
  private final String rmapiQuery;
  private final String rmapiMessage;
  private final String responseBody;

  public RMAPIServiceException(String message, Integer rmapiCode, String rmapiMessage, String responseBody,
      String rmapiQuery) {
    super(message);
    this.rmapiCode = rmapiCode;
    this.rmapiMessage = rmapiMessage;
    this.responseBody = responseBody;
    this.rmapiQuery = rmapiQuery;
  }

  public Integer getRMAPICode() {
    return this.rmapiCode;
  }

  public String getRMAPIMessage() {
    return this.rmapiMessage;
  }

  public String getResponseBody() {
    return this.responseBody;
  }

  public String getRMAPIQuery() {
    return this.rmapiQuery;
  }
}
