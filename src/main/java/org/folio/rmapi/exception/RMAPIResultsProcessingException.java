package org.folio.rmapi.exception;

/**
 * @author cgodfrey
 *
 */
public class RMAPIResultsProcessingException extends RMAPIException {

  private static final long serialVersionUID = 1L;

  public RMAPIResultsProcessingException(String message, Exception e) {
    super(message, e);
  }
}
