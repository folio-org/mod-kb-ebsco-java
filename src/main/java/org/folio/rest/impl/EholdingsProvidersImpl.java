package org.folio.rest.impl;

import java.util.Map;

import javax.validation.ValidationException;
import javax.ws.rs.core.Response;

import org.folio.config.api.RMAPIConfigurationService;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.VendorConverter;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderCollection;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.resource.EholdingsProviders;
import org.folio.rest.model.Sort;
import org.folio.rest.parser.IdParser;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.rest.validator.HeaderValidator;
import org.folio.rest.validator.PackageParametersValidator;
import org.folio.rest.validator.ProviderPutBodyValidator;
import org.folio.rmapi.exception.RMAPIResourceNotFoundException;
import org.folio.rmapi.model.VendorById;
import org.folio.rmapi.model.VendorPut;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EholdingsProvidersImpl implements EholdingsProviders {

  private static final String GET_PROVIDER_NOT_FOUND_MESSAGE = "Provider not found";

  @Autowired
  private VendorConverter converter;
  @Autowired
  private ProviderPutBodyValidator bodyValidator;
  @Autowired
  private PackageParametersValidator parametersValidator;
  @Autowired
  private IdParser idParser;
  @Autowired
  private RMAPITemplateFactory templateFactory;

  public EholdingsProvidersImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsProviders(String q, String sort, int page, int count, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    validateSort(sort);
    validateQuery(q);

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction((rmapiService, okapiData) ->
        rmapiService.retrieveProviders(q, page, count, Sort.valueOf(sort.toUpperCase()))
      )
      .executeWithResult(ProviderCollection.class);
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsProvidersByProviderId(String providerId, String include, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    long providerIdLong = idParser.parseProviderId(providerId);

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction((rmapiService, okapiData) ->
        rmapiService.retrieveProvider(providerIdLong, include)
      )
      .addErrorMapper(RMAPIResourceNotFoundException.class, exception ->
        GetEholdingsProvidersByProviderIdResponse.respond404WithApplicationVndApiJson(
          ErrorUtil.createError(GET_PROVIDER_NOT_FOUND_MESSAGE)))
      .executeWithResult(Provider.class);
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsProvidersByProviderId(String providerId, String contentType, ProviderPutRequest entity,
                                                Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    long providerIdLong = idParser.parseProviderId(providerId);

    bodyValidator.validate(entity);

    VendorPut rmapiVendor = converter.convertToVendor(entity);

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction((rmapiService, okapiData) ->
        rmapiService.updateProvider(providerIdLong, rmapiVendor)
      )
      .executeWithResult(Provider.class);
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsProvidersPackagesByProviderId(String providerId, String q, String filterSelected,
                                                        String filterType, String sort, int page, int count,
                                                        Map<String, String> okapiHeaders,
                                                        Handler<AsyncResult<Response>> asyncResultHandler,
                                                        Context vertxContext) {
    long providerIdLong = idParser.parseProviderId(providerId);
    parametersValidator.validate("true", filterSelected, filterType, sort, q);

    Sort nameSort = Sort.valueOf(sort.toUpperCase());

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction((rmapiService, okapiData) ->
        rmapiService.retrievePackages(filterSelected, filterType, providerIdLong, q, page, count, nameSort)
      )
      .addErrorMapper(RMAPIResourceNotFoundException.class, exception ->
        GetEholdingsProvidersPackagesByProviderIdResponse.respond404WithApplicationVndApiJson(
          ErrorUtil.createError(GET_PROVIDER_NOT_FOUND_MESSAGE)
        ))
      .executeWithResult(PackageCollection.class);
  }

  private void validateSort(String sort) {
    if (!Sort.contains(sort.toUpperCase())) {
      throw new ValidationException("Invalid sort parameter");
    }
  }

  private void validateQuery(String query) {
    if ("".equals(query)) {
      throw new ValidationException("Search parameter cannot be empty");
    }
  }
}
