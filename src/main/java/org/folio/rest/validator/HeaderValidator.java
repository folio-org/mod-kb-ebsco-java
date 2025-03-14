package org.folio.rest.validator;

import jakarta.validation.ValidationException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.folio.okapi.common.XOkapiHeaders;
import org.springframework.stereotype.Component;

/**
 * Verifies that headers are valid.
 */
@Component
public class HeaderValidator {

  private static final Collection<String> EXPECTED_HEADERS = Collections.singletonList(
    XOkapiHeaders.URL
  );

  /**
   * Validate headers.
   *
   * @param okapiHeaders request headers
   * @throws ValidationException if validation failed
   */
  public void validate(Map<String, String> okapiHeaders) {
    for (String header : EXPECTED_HEADERS) {
      if (!okapiHeaders.containsKey(header)) {
        throw new ValidationException("Missing header " + header);
      }
    }
  }
}
