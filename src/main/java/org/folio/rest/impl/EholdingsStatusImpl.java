package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.factory.annotation.Autowired;

import org.folio.holdingsiq.model.OkapiData;
import org.folio.holdingsiq.service.ConfigurationService;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.configuration.StatusConverter;
import org.folio.rest.jaxrs.resource.EholdingsStatus;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.validator.HeaderValidator;
import org.folio.spring.SpringContextUtil;

public class EholdingsStatusImpl implements EholdingsStatus {

  @Autowired
  private ConfigurationService configurationService;
  @Autowired
  private HeaderValidator headerValidator;
  @Autowired
  private StatusConverter converter;

  public EholdingsStatusImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsStatus(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    MutableObject<OkapiData> okapiData = new MutableObject<>();
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> {
        okapiData.setValue(new OkapiData(okapiHeaders));
        return configurationService.retrieveConfiguration(okapiData.getValue());
      })
      .thenCompose(configuration -> configurationService.verifyCredentials(configuration, vertxContext, okapiData.getValue()))
      .thenAccept(errors -> asyncResultHandler.handle(Future.succeededFuture(GetEholdingsStatusResponse.respond200WithApplicationVndApiJson(converter.convert(errors.isEmpty())))))
      .exceptionally(e -> {
        new ErrorHandler().handle(asyncResultHandler, e);
        return null;
      });
  }

}
