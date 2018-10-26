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
import org.folio.rest.annotations.Validate;
import org.folio.rest.converter.VendorConverter;
import org.folio.rest.jaxrs.model.EholdingsProvidersPackagesGetFilterSelected;
import org.folio.rest.jaxrs.model.EholdingsProvidersPackagesGetFilterType;
import org.folio.rest.jaxrs.model.EholdingsProvidersProviderIdGetInclude;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.resource.EholdingsProviders;
import org.folio.rest.model.OkapiData;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.validator.HeaderValidator;
import org.folio.rmapi.RMAPIService;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EholdingsProvidersImpl implements EholdingsProviders {

  private static final String GET_PROVIDERS_ERROR_MESSAGE = "Failed to retrieve providers";

  private final Logger logger = LoggerFactory.getLogger(EholdingsConfigurationImpl.class);

  private RMAPIConfigurationService configurationService;
  private HeaderValidator headerValidator;
  private VendorConverter converter;

  public EholdingsProvidersImpl() {
    this(
      new RMAPIConfigurationServiceCache(
        new RMAPIConfigurationServiceImpl(new ConfigurationClientProvider())),
      new HeaderValidator(),
      new VendorConverter());
  }

  public EholdingsProvidersImpl(RMAPIConfigurationService configurationService,
                                HeaderValidator headerValidator,
                                VendorConverter converter) {
    this.configurationService = configurationService;
    this.headerValidator = headerValidator;
    this.converter = converter;
  }

  @Override
  @Validate
  public void getEholdingsProviders(String q, int page, int count, String sort, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if (!headerValidator.validate(okapiHeaders, asyncResultHandler)) {
      return;
    }
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> configurationService.retrieveConfiguration(new OkapiData(okapiHeaders)))
      .thenCompose(rmapiConfiguration -> {
        RMAPIService rmapiService = new RMAPIService(rmapiConfiguration.getCustomerId(), rmapiConfiguration.getAPIKey(),
          rmapiConfiguration.getUrl(), vertxContext.owner());
        return rmapiService.retrieveProviders(q, page, count, sort);
      })
      .thenAccept(vendors ->
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersResponse
        .respond200WithApplicationVndApiJson(converter.convert(vendors)))))
      .exceptionally(e -> {
        logger.error(GET_PROVIDERS_ERROR_MESSAGE, e);
        asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersResponse
          .status(500)
          .header("Content-Type", "application/vnd.api+json")
          .entity(ErrorUtil.createError(e.getCause().getMessage()))
          .build()));
        return null;
      });
  }

  @Override
  public void getEholdingsProvidersByProviderId(String providerId, EholdingsProvidersProviderIdGetInclude include, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersResponse.status(Response.Status.NOT_IMPLEMENTED).build()));
  }

  @Override
  public void putEholdingsProvidersByProviderId(String providerId, String contentType, ProviderPutRequest entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersResponse.status(Response.Status.NOT_IMPLEMENTED).build()));
  }

  @Override
  public void getEholdingsProvidersPackages(String q, int page, int count, String sort, EholdingsProvidersPackagesGetFilterSelected filterSelected, EholdingsProvidersPackagesGetFilterType filterType, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    asyncResultHandler.handle(Future.succeededFuture(GetEholdingsProvidersResponse.status(Response.Status.NOT_IMPLEMENTED).build()));
  }
}
