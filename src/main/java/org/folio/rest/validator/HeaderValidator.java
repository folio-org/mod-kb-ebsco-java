package org.folio.rest.validator;

import org.folio.rest.util.RestConstants;

import javax.validation.ValidationException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Verifies that headers are valid
 */
public class HeaderValidator {

  private final Collection<String> expectedHeaders = Arrays.asList(
    RestConstants.OKAPI_URL_HEADER
  );

  /**
   * @param okapiHeaders request headers
   * @throws ValidationException if validation failed
   */
  public void validate(Map<String, String> okapiHeaders) {
    for (String header : expectedHeaders) {
      if (!okapiHeaders.containsKey(header)) {
        throw new ValidationException("Missing header " + header);
      }
    }
  }
}
