package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.mutable.MutableObject;
import org.folio.config.RMAPIConfiguration;
import org.folio.config.RMAPIConfigurationServiceCache;
import org.folio.config.RMAPIConfigurationServiceImpl;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.config.exception.RMAPIConfigurationInvalidException;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.RMAPIConfigurationConverter;
import org.folio.rest.jaxrs.model.Configuration;
import org.folio.rest.jaxrs.model.ConfigurationPutRequest;
import org.folio.rest.jaxrs.resource.EholdingsConfiguration;
import org.folio.rest.model.OkapiData;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.HeaderValidator;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EholdingsConfigurationImpl implements EholdingsConfiguration {

  private static final String UPDATE_ERROR_MESSAGE = "Failed to update configuration";
  private final Logger logger = LoggerFactory.getLogger(EholdingsConfigurationImpl.class);

  private RMAPIConfigurationService configurationService;
  private RMAPIConfigurationConverter converter;
  private HeaderValidator headerValidator;

  public EholdingsConfigurationImpl() {
    this(
         new RMAPIConfigurationServiceCache(
           new RMAPIConfigurationServiceImpl(new ConfigurationClientProvider())),
         new RMAPIConfigurationConverter(), new HeaderValidator());
  }

  public EholdingsConfigurationImpl(RMAPIConfigurationService configurationService, RMAPIConfigurationConverter converter, HeaderValidator headerValidator) {
    this.configurationService = configurationService;
    this.converter = converter;
    this.headerValidator = headerValidator;
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsConfiguration(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders), vertxContext))
      .thenAccept(rmapiConfiguration -> {
        Configuration configuration = converter.convertToConfiguration(rmapiConfiguration);
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsConfigurationResponse.respond200WithApplicationVndApiJson(configuration)));
      })
      .exceptionally(e -> {
        logger.error(UPDATE_ERROR_MESSAGE, e);
        new ErrorHandler()
          .addDefaultMapper()
          .handle(asyncResultHandler, e);
        return null;
      });
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsConfiguration(String contentType, ConfigurationPutRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    MutableObject<OkapiData> okapiData = new MutableObject<>();
    RMAPIConfiguration rmapiConfiguration = converter.convertToRMAPIConfiguration(entity);
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> {
        okapiData.setValue(new OkapiData(okapiHeaders));
        return configurationService.verifyCredentials(rmapiConfiguration, vertxContext, okapiData.getValue().getTenant());
      })
      .thenCompose(errors -> {
        if (!errors.isEmpty()) {
          CompletableFuture<Object> future = new CompletableFuture<>();
          future.completeExceptionally(new RMAPIConfigurationInvalidException(errors));
          return future;
        }
        return CompletableFuture.completedFuture(null);
      })
      .thenCompose(o -> configurationService.updateConfiguration(rmapiConfiguration, vertxContext, okapiData.getValue()))
      .thenAccept(configuration ->
        asyncResultHandler.handle(Future.succeededFuture(PutEholdingsConfigurationResponse
          .respond200WithApplicationVndApiJson(converter.convertToConfiguration(rmapiConfiguration)))))
      .exceptionally(e -> {
        new ErrorHandler()
          .add(RMAPIConfigurationInvalidException.class, exception ->
            EholdingsConfiguration.PutEholdingsConfigurationResponse
              .respond422WithApplicationVndApiJson(ErrorUtil.createError(exception.getErrors().get(0).getMessage())))
          .addDefaultMapper()
          .handle(asyncResultHandler, e);
        return null;
      });
  }
}
