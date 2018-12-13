package org.folio.rest.util.template;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.config.api.RMAPIConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@Component
public class RMAPITemplateFactory {
  @Autowired
  private RMAPIConfigurationService configurationService;
  @Autowired
  private Vertx vertx;
  @Autowired
  private ConversionService conversionService;

  public RMAPITemplate createTemplate(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler){
    return new RMAPITemplate(configurationService, vertx, conversionService, okapiHeaders, asyncResultHandler);
  }
}
