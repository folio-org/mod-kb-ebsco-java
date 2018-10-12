package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.config.RMAPIConfigurationClient;
import org.folio.http.ConfigurationClientProvider;
import org.folio.rest.converter.RMAPIConfigurationConverter;
import org.folio.rest.jaxrs.model.EholdingsConfigurationPutApplicationVndApiJson;
import org.folio.rest.jaxrs.model.EholdingsConfigurationPutApplicationVndApiJsonImpl;
import org.folio.rest.jaxrs.resource.EholdingsConfiguration;
import org.folio.rest.util.HeaderConstants;
import org.folio.rest.validator.HeaderValidator;

import javax.ws.rs.core.Response;
import java.util.Map;

public class EholdingsConfigurationImpl implements EholdingsConfiguration {

  private final Logger logger = LoggerFactory.getLogger(EholdingsConfigurationImpl.class);

  private RMAPIConfigurationClient configurationClient;
  private RMAPIConfigurationConverter converter;
  private HeaderValidator headerValidator;

  public EholdingsConfigurationImpl() {
    this(new RMAPIConfigurationClient(new ConfigurationClientProvider()), new RMAPIConfigurationConverter(), new HeaderValidator());
  }

  public EholdingsConfigurationImpl(RMAPIConfigurationClient configurationClient, RMAPIConfigurationConverter converter, HeaderValidator headerValidator) {
    this.configurationClient = configurationClient;
    this.converter = converter;
    this.headerValidator = headerValidator;
  }

  @Override
  public void getEholdingsConfiguration(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if (!headerValidator.validate(okapiHeaders, asyncResultHandler)) {
      return;
    }
    configurationClient.retrieveConfiguration(
      okapiHeaders.get(HeaderConstants.OKAPI_TOKEN_HEADER),
      okapiHeaders.get(HeaderConstants.OKAPI_TENANT_HEADER),
      okapiHeaders.get(HeaderConstants.OKAPI_URL_HEADER))
      .thenAccept(rmAPIconfiguration -> {
        EholdingsConfigurationPutApplicationVndApiJsonImpl configuration = converter.convert(rmAPIconfiguration);
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsConfigurationResponse.respond200WithApplicationVndApiJson(configuration)));
      })
      .exceptionally(e -> {
        logger.error("Failed to get configuration", e);
        asyncResultHandler.handle(Future.failedFuture(e));
        return null;
      });
  }

  @Override
  public void putEholdingsConfiguration(String contentType, EholdingsConfigurationPutApplicationVndApiJson entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(PutEholdingsConfigurationResponse.status(Response.Status.NOT_IMPLEMENTED).build()));
  }
}
