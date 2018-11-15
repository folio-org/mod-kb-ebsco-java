package org.folio.rmapi.exception;

/**
 * @author cgodfrey
 *
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class RMAPIUnAuthorizedException extends RMAPIServiceException {

  private static final long serialVersionUID = 1L;

  public RMAPIUnAuthorizedException(String message, Integer rmapiCode, String rmapiMessage, String responseBody,
                                    String rmapiQuery) {
    super(message, rmapiCode, rmapiMessage, responseBody, rmapiQuery);
  }
}
