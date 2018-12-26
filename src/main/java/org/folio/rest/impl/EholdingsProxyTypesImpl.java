package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.http.HttpConsts.CONTENT_TYPE_HEADER;
import static org.folio.http.HttpConsts.JSON_API_TYPE;
import static org.folio.rest.jaxrs.resource.EholdingsProxyTypes.GetEholdingsProxyTypesResponse.respond200WithApplicationVndApiJson;
import static org.folio.rest.util.ErrorUtil.createError;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.ProxyConverter;
import org.folio.rest.jaxrs.model.ProxyTypes;
import org.folio.rest.jaxrs.resource.EholdingsProxyTypes;
import org.folio.rest.model.OkapiData;
import org.folio.rest.validator.HeaderValidator;
import org.folio.rmapi.RMAPIService;
import org.folio.rmapi.exception.RMAPIUnAuthorizedException;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EholdingsProxyTypesImpl implements EholdingsProxyTypes {

  private final Logger logger = LoggerFactory.getLogger(EholdingsProxyTypesImpl.class);

  @Autowired
  private RMAPIConfigurationService configurationService;
  @Autowired
  private ProxyConverter converter;
  @Autowired
  private HeaderValidator headerValidator;

  public EholdingsProxyTypesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  public EholdingsProxyTypesImpl(RMAPIConfigurationService configurationService, ProxyConverter converter,
    HeaderValidator headerValidator) {
    this.configurationService = configurationService;
    this.converter = converter;
    this.headerValidator = headerValidator;
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsProxyTypes(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    headerValidator.validate(okapiHeaders);

    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner());

        return rmapiService.retrieveProxies();
      })
      .thenAccept(proxies -> {
        ProxyTypes proxyTypes = converter.convert(proxies);

        asyncResultHandler.handle(succeededFuture(respond200WithApplicationVndApiJson(proxyTypes)));
      })
      .exceptionally(e -> {
        logger.error("Failed to get proxy types", e);

        if(e.getCause() instanceof RMAPIUnAuthorizedException){
          RMAPIUnAuthorizedException rmApiException = (RMAPIUnAuthorizedException)e.getCause();

          asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProxyTypesResponse
            .status(HttpStatus.SC_FORBIDDEN)
            .header(CONTENT_TYPE_HEADER, JSON_API_TYPE)
            .entity(createError(rmApiException.getMessage()))
            .build()));
        }
        else {
          asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProxyTypesResponse
            .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            .header(CONTENT_TYPE_HEADER, JSON_API_TYPE)
            .entity(createError(e.getCause().getMessage()))
            .build()));
        }
        return null;
      });
  }
}
