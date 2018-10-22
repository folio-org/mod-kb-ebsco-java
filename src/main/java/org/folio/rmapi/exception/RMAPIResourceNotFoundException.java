package org.folio.rmapi.exception;

/**
 * @author cgodfrey
 *
 */
public class RMAPIResourceNotFoundException extends RMAPIException {

  private static final long serialVersionUID = 1L;

  public RMAPIResourceNotFoundException(String message) {
    super(message);
  }
}
