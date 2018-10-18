package org.folio.rest.validator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.folio.rest.jaxrs.resource.EholdingsConfiguration;
import org.folio.rest.util.HeaderConstants;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Verifies that headers are valid
 */
public class HeaderValidator {

  private final Collection<String> expectedHeaders = Arrays.asList(
    HeaderConstants.OKAPI_URL_HEADER
  );

  /**
   * @param okapiHeaders request headers
   * @param asyncResultHandler handler that will be called with error response if headers are invalid
   * @return true if request is valid
   */
  public boolean validate(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler) {
    for (String header : expectedHeaders) {
      if (!okapiHeaders.containsKey(header)) {
        asyncResultHandler.handle(Future.succeededFuture(
          EholdingsConfiguration.GetEholdingsConfigurationResponse
            .status(400)
            .entity("Missing header " + header)
            .build()));
        return false;
      }
    }
    return true;
  }
}
