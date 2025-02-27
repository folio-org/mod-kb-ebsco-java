package org.folio.rest.util.template;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.validator.HeaderValidator;
import org.folio.service.kbcredentials.UserKbCredentialsService;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

@Component
public class RmApiTemplateFactory {

  private final ConversionService conversionService;
  private final HeaderValidator headerValidator;

  public RmApiTemplateFactory(ConversionService conversionService, HeaderValidator headerValidator) {
    this.conversionService = conversionService;
    this.headerValidator = headerValidator;
  }

  public RmApiTemplate createTemplate(Map<String, String> okapiHeaders,
                                      Handler<AsyncResult<Response>> asyncResultHandler) {
    return new RmApiTemplate(
      lookupUserKbCredentialsService(), conversionService, headerValidator,
      lookupContextBuilder(), okapiHeaders, asyncResultHandler);
  }

  /**
   * Because of @Lookup annotation this method will be overridden by Spring,
   * and new instance of context builder will be returned.
   */
  @Lookup
  public RmApiTemplateContextBuilder lookupContextBuilder() {
    return null;
  }

  @Lookup("nonSecuredUserCredentialsService")
  public UserKbCredentialsService lookupUserKbCredentialsService() {
    return null;
  }
}
