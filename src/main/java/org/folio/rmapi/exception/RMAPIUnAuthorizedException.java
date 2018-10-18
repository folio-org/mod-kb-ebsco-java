package org.folio.rmapi.exception;

/**
 * @author cgodfrey
 *
 */
public class RMAPIUnAuthorizedException extends RMAPIException {

  private static final long serialVersionUID = 1L;

  public RMAPIUnAuthorizedException(String message) {
    super(message);
  }
}
