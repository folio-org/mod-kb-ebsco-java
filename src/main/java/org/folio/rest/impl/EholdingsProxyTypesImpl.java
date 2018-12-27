package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.Response.ok;
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
import org.folio.rest.converter.ProxyTypesConverter;
import org.folio.rest.jaxrs.model.ProxyTypes;
import org.folio.rest.jaxrs.resource.EholdingsProxyTypes;
import org.folio.rest.model.OkapiData;
import org.folio.rest.util.template.RMAPITemplateFactory;
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
  @Autowired
  private RMAPITemplateFactory rmapiTemplateFactory;

  public EholdingsProxyTypesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsProxyTypes(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    rmapiTemplateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction((rmapiService, okapiData) ->
        rmapiService.retrieveProxies()
      )
      .addErrorMapper(RMAPIUnAuthorizedException.class, rmApiException -> GetEholdingsProxyTypesResponse
        .status(HttpStatus.SC_FORBIDDEN)
        .header(CONTENT_TYPE_HEADER, JSON_API_TYPE)
        .entity(createError(rmApiException.getMessage()))
        .build())
      .executeWithResult(ProxyTypes.class);
  }
}
