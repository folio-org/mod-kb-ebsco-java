package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.config.RMAPIConfiguration;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.config.RMAPIConfigurationServiceImpl;
import org.folio.config.exception.RMAPIConfigurationInvalidException;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.converter.RMAPIConfigurationConverter;
import org.folio.rest.jaxrs.model.Configuration;
import org.folio.rest.jaxrs.model.ConfigurationPutRequest;
import org.folio.rest.jaxrs.model.ConfigurationUnprocessableError;
import org.folio.rest.jaxrs.resource.EholdingsConfiguration;
import org.folio.rest.model.OkapiData;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.HeaderValidator;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EholdingsConfigurationImpl implements EholdingsConfiguration {

  private static final String UPDATE_ERROR_MESSAGE = "Failed to update configuration";
  private static final String INTERNAL_SERVER_ERROR = "Internal server error";
  private static final String CONFIGURATION_IS_INVALID_ERROR = "Configuration is invalid";
  private final Logger logger = LoggerFactory.getLogger(EholdingsConfigurationImpl.class);

  private RMAPIConfigurationService configurationService;
  private RMAPIConfigurationConverter converter;
  private HeaderValidator headerValidator;

  public EholdingsConfigurationImpl() {
    this(new RMAPIConfigurationServiceImpl(new ConfigurationClientProvider()), new RMAPIConfigurationConverter(), new HeaderValidator());
  }

  public EholdingsConfigurationImpl(RMAPIConfigurationService configurationService, RMAPIConfigurationConverter converter, HeaderValidator headerValidator) {
    this.configurationService = configurationService;
    this.converter = converter;
    this.headerValidator = headerValidator;
  }

  @Override
  public void getEholdingsConfiguration(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      if (!headerValidator.validate(okapiHeaders, asyncResultHandler)) {
        return;
      }
      OkapiData okapiData = new OkapiData(okapiHeaders);
      configurationService.retrieveConfiguration(okapiData)
        .thenAccept(rmapiConfiguration -> {
          Configuration configuration = converter.convertToConfiguration(rmapiConfiguration);
          asyncResultHandler.handle(Future.succeededFuture(GetEholdingsConfigurationResponse.respond200WithApplicationVndApiJson(configuration)));
        })
        .exceptionally(e -> {
          logger.error(UPDATE_ERROR_MESSAGE, e);
          asyncResultHandler.handle(Future.succeededFuture(GetEholdingsConfigurationResponse.respond500WithTextPlain(INTERNAL_SERVER_ERROR)));
          return null;
        });
    } catch (RuntimeException e) {
      logger.error(UPDATE_ERROR_MESSAGE, e);
      asyncResultHandler.handle(Future.succeededFuture(EholdingsConfiguration.PutEholdingsConfigurationResponse
        .respond500WithTextPlain(INTERNAL_SERVER_ERROR)));
    }
  }

  @Override
  public void putEholdingsConfiguration(String contentType, ConfigurationPutRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      if (!headerValidator.validate(okapiHeaders, asyncResultHandler)) {
        return;
      }
      OkapiData okapiData = new OkapiData(okapiHeaders);
      RMAPIConfiguration rmapiConfiguration = converter.convertToRMAPIConfiguration(entity);

      configurationService
        .verifyCredentials(rmapiConfiguration, vertxContext)
        .thenCompose(isValid -> {
          CompletableFuture<Object> future = new CompletableFuture<>();
          future.completeExceptionally(new RMAPIConfigurationInvalidException());
          return future;
        })
        .thenCompose(o -> configurationService.updateConfiguration(rmapiConfiguration, okapiData))
        .thenAccept(configuration ->
          asyncResultHandler.handle(Future.succeededFuture(PutEholdingsConfigurationResponse
            .respond200WithApplicationVndApiJson(converter.convertToConfiguration(rmapiConfiguration)))))
        .exceptionally(e -> {
          if (e.getCause() instanceof RMAPIConfigurationInvalidException) {
            ConfigurationUnprocessableError configurationError = ErrorUtil.createError(CONFIGURATION_IS_INVALID_ERROR);
            asyncResultHandler.handle(Future.succeededFuture(EholdingsConfiguration.PutEholdingsConfigurationResponse.respond422WithApplicationVndApiJson(configurationError)));
          } else {
            logger.error(UPDATE_ERROR_MESSAGE, e);
            asyncResultHandler.handle(Future.succeededFuture(EholdingsConfiguration.PutEholdingsConfigurationResponse
              .respond500WithTextPlain(INTERNAL_SERVER_ERROR)));
          }
          return null;
        });
    } catch (RuntimeException e) {
      logger.error(UPDATE_ERROR_MESSAGE, e);
      asyncResultHandler.handle(Future.succeededFuture(EholdingsConfiguration.PutEholdingsConfigurationResponse
        .respond500WithTextPlain(INTERNAL_SERVER_ERROR)));
    }
  }
}
