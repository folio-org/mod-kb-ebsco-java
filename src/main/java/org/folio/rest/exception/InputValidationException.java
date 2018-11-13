package org.folio.rest.exception;

public class InputValidationException extends RuntimeException {

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