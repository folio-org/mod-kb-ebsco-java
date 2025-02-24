package org.folio.service.holdings.exception;

import java.io.Serial;

public class ProcessInProgressException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = -94932377914823017L;

  public ProcessInProgressException(String message) {
    super(message);
  }
}
