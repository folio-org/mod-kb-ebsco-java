package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.mutable.MutableObject;
import org.folio.config.RMAPIConfiguration;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.config.exception.RMAPIConfigurationInvalidException;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.configuration.RMAPIConfigurationConverter;
import org.folio.rest.jaxrs.model.Configuration;
import org.folio.rest.jaxrs.model.ConfigurationPutRequest;
import org.folio.rest.jaxrs.resource.EholdingsConfiguration;
import org.folio.rest.model.OkapiData;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.ConfigurationPutBodyValidator;
import org.folio.rest.validator.HeaderValidator;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EholdingsConfigurationImpl implements EholdingsConfiguration {

  private static final String UPDATE_ERROR_MESSAGE = "Failed to update configuration";
  private final Logger logger = LoggerFactory.getLogger(EholdingsConfigurationImpl.class);

  @Autowired
  private RMAPIConfigurationService configurationService;
  @Autowired
  private RMAPIConfigurationConverter converter;
  @Autowired
  private HeaderValidator headerValidator;
  @Autowired
  private ConfigurationPutBodyValidator bodyValidator;

  public EholdingsConfigurationImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsConfiguration(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
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
    bodyValidator.validate(entity);
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
      .thenCompose(o -> configurationService.updateConfiguration(rmapiConfiguration, okapiData.getValue()))
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
