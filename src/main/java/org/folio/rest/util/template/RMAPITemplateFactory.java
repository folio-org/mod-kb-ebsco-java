package org.folio.rest.util.template;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import org.folio.rest.validator.HeaderValidator;
import org.folio.service.kbcredentials.UserKbCredentialsService;

@Component
public class RMAPITemplateFactory {

  @Autowired
  private ConversionService conversionService;
  @Autowired
  private HeaderValidator headerValidator;

  public RMAPITemplate createTemplate(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler) {
    return new RMAPITemplate(
        lookupUserKbCredentialsService(), conversionService, headerValidator,
        lookupContextBuilder(), okapiHeaders, asyncResultHandler);
  }

  /**
   * Because of @Lookup annotation this method will be overridden by Spring, and new instance of context builder will be returned
   */
  @Lookup
  public RMAPITemplateContextBuilder lookupContextBuilder(){
    return null;
  }

  @Lookup("nonSecuredUserCredentialsService")
  public UserKbCredentialsService lookupUserKbCredentialsService() {
    return null;
  }
}
