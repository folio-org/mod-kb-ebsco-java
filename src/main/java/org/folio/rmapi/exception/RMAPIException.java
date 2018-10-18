package org.folio.rmapi.exception;

public class RMAPIException extends RuntimeException {
  public RMAPIException(String message) {
    super(message);
  }

  public RMAPIException(String message, Throwable cause) {
    super(message, cause);
  }
}
