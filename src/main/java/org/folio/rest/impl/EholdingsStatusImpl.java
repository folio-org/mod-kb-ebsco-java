package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.mutable.MutableObject;
import org.folio.config.RMAPIConfigurationServiceCache;
import org.folio.config.RMAPIConfigurationServiceImpl;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.StatusConverter;
import org.folio.rest.jaxrs.resource.EholdingsStatus;
import org.folio.rest.model.OkapiData;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.validator.HeaderValidator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EholdingsStatusImpl implements EholdingsStatus {

  private static final String INTERNAL_SERVER_ERROR = "Internal server error";
  private final Logger logger = LoggerFactory.getLogger(EholdingsStatusImpl.class);
  private RMAPIConfigurationService configurationService;
  private HeaderValidator headerValidator;
  private StatusConverter converter;

  public EholdingsStatusImpl() {
    this(
      new RMAPIConfigurationServiceCache(
        new RMAPIConfigurationServiceImpl(new ConfigurationClientProvider())),
      new HeaderValidator(),
      new StatusConverter());
  }

  public EholdingsStatusImpl(RMAPIConfigurationService configurationService, HeaderValidator headerValidator, StatusConverter converter) {
    this.configurationService = configurationService;
    this.headerValidator = headerValidator;
    this.converter = converter;
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsStatus(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    MutableObject<OkapiData> okapiData = new MutableObject<>();
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> {
        okapiData.setValue(new OkapiData(okapiHeaders));
        return configurationService.retrieveConfiguration(okapiData.getValue(), vertxContext);
      })
      .thenCompose(configuration -> configurationService.verifyCredentials(configuration, vertxContext, okapiData.getValue().getTenant()))
      .thenAccept(errors -> asyncResultHandler.handle(Future.succeededFuture(GetEholdingsStatusResponse.respond200WithApplicationVndApiJson(converter.convert(errors.isEmpty())))))
      .exceptionally(e -> {
        logger.error(INTERNAL_SERVER_ERROR, e);
        new ErrorHandler()
          .addDefaultMapper()
          .handle(asyncResultHandler, e);
        return null;
      });
  }

}
