package org.folio.rest.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.validation.ValidationException;
import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import org.folio.holdingsiq.model.Sort;
import org.folio.holdingsiq.model.VendorById;
import org.folio.holdingsiq.model.VendorPut;
import org.folio.holdingsiq.service.exception.ResourceNotFoundException;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderCollection;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.resource.EholdingsProviders;
import org.folio.rest.parser.IdParser;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.rest.validator.PackageParametersValidator;
import org.folio.rest.validator.ProviderPutBodyValidator;
import org.folio.rmapi.result.VendorResult;
import org.folio.spring.SpringContextUtil;
import org.folio.tag.RecordType;
import org.folio.tag.repository.TagRepository;

public class EholdingsProvidersImpl implements EholdingsProviders {

  private static final String GET_PROVIDER_NOT_FOUND_MESSAGE = "Provider not found";

  @Autowired
  private Converter<ProviderPutRequest, VendorPut> putRequestConverter;
  @Autowired
  private ProviderPutBodyValidator bodyValidator;
  @Autowired
  private PackageParametersValidator parametersValidator;
  @Autowired
  private IdParser idParser;
  @Autowired
  private RMAPITemplateFactory templateFactory;
  @Autowired
  private TagRepository tagRepository;

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
      .requestAction(context ->
        context.getProvidersService().retrieveProviders(q, page, count, Sort.valueOf(sort.toUpperCase()))
      )
      .executeWithResult(ProviderCollection.class);
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsProvidersByProviderId(String providerId, String include, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    long providerIdLong = idParser.parseProviderId(providerId);

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getProvidersService().retrieveProvider(providerIdLong, include)
        .thenCompose(result ->
          loadTags(result, context.getOkapiData().getTenant())
        )
      )
      .addErrorMapper(ResourceNotFoundException.class, exception ->
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

    VendorPut rmapiVendor = putRequestConverter.convert(entity);

    final Tags tags = entity.getData().getAttributes().getTags();
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context -> context.getProvidersService().updateProvider(providerIdLong, rmapiVendor)
        .thenCompose(result ->
          updateTags(result, context.getOkapiData().getTenant(), tags))
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
      .requestAction(context ->
        context.getPackagesService().retrievePackages(filterSelected, filterType, providerIdLong, q, page, count, nameSort)
      )
      .addErrorMapper(ResourceNotFoundException.class, exception ->
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

  private CompletableFuture<VendorResult> loadTags(VendorResult result, String tenant) {
    return tagRepository.getTags(tenant, String.valueOf(result.getVendor().getVendorId()), RecordType.PROVIDER)
      .thenCompose(tag -> {
        result.setTags(tag);
        return CompletableFuture.completedFuture(result);
      });
  }

  private CompletableFuture<VendorResult> updateTags(VendorById vendorById, String tenant, Tags tags) {
    if (tags == null){
      return CompletableFuture.completedFuture(new VendorResult(vendorById, null));
    }else {
      return tagRepository.updateTags(tenant, String.valueOf(vendorById.getVendorId()), RecordType.PROVIDER, tags.getTagList())
        .thenCompose(updated -> {
          VendorResult result = new VendorResult(vendorById, null);
          result.setTags(new Tags().withTagList(tags.getTagList()));
          return CompletableFuture.completedFuture(result);
        });
    }
  }
}
