package org.folio.rest.util.template;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.service.ConfigurationService;
import org.folio.rest.validator.HeaderValidator;

@Component
public class RMAPITemplateFactory {
  @Autowired
  private ConfigurationService configurationService;
  @Autowired
  private Vertx vertx;
  @Autowired
  private ConversionService conversionService;
  @Autowired
  private HeaderValidator headerValidator;
  @Autowired
  private RMAPIServicesFactory servicesFactory;

  public RMAPITemplate createTemplate(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler){
    return new RMAPITemplate(servicesFactory, configurationService, vertx, conversionService, headerValidator, okapiHeaders, asyncResultHandler);
  }
}
