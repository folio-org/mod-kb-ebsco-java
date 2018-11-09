package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.config.RMAPIConfigurationServiceCache;
import org.folio.config.RMAPIConfigurationServiceImpl;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.StatusConverter;
import org.folio.rest.jaxrs.resource.EholdingsStatus;
import org.folio.rest.model.OkapiData;
import org.folio.rest.validator.HeaderValidator;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> CompletableFuture.completedFuture(new OkapiData(okapiHeaders)))
      .thenCompose(okapiData -> configurationService.retrieveConfiguration(okapiData))
      .thenCompose(configuration -> configurationService.verifyCredentials(configuration, vertxContext))
      .thenAccept(isValid -> asyncResultHandler.handle(Future.succeededFuture(GetEholdingsStatusResponse.respond200WithApplicationVndApiJson(converter.convert(isValid)))))
      .exceptionally(e -> {
        logger.error(INTERNAL_SERVER_ERROR, e);
        asyncResultHandler.handle(Future.succeededFuture(
          EholdingsStatus.GetEholdingsStatusResponse.respond500WithTextPlain(INTERNAL_SERVER_ERROR)));
        return null;
      });
  }

}
