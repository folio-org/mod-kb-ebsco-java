package org.folio.rest.exception;

import java.io.Serial;

public class InputValidationException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  private final String messageDetail;

  public InputValidationException(String message, String messageDetail) {
    super(message);
    this.messageDetail = messageDetail;
  }

  public String getMessageDetail() {
    return this.messageDetail;
  }
}
