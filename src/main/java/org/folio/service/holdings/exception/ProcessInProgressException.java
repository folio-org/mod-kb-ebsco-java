package org.folio.service.holdings.exception;

public class ProcessInProgressException extends RuntimeException {
  private static final long serialVersionUID = -94932377914823017L;

  public ProcessInProgressException(String message) {
    super(message);
  }
}
