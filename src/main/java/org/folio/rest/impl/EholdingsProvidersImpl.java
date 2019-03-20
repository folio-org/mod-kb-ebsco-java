package org.folio.rest.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.validation.ValidationException;
import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import org.folio.holdingsiq.model.Sort;
import org.folio.holdingsiq.model.Vendor;
import org.folio.holdingsiq.model.VendorById;
import org.folio.holdingsiq.model.VendorPut;
import org.folio.holdingsiq.model.Vendors;
import org.folio.holdingsiq.service.exception.ResourceNotFoundException;
import org.folio.holdingsiq.service.validator.PackageParametersValidator;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderCollection;
import org.folio.rest.jaxrs.model.ProviderDataAttributes;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.resource.EholdingsProviders;
import org.folio.rest.parser.IdParser;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.RestConstants;
import org.folio.rest.util.template.RMAPITemplate;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.rest.util.template.RMAPITemplateFactory;
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
  public void getEholdingsProviders(String q, String sortString, String filterTags, int page, int count,
                                    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
                                    Context vertxContext) {
    validateSort(sortString);
    Sort sort = Sort.valueOf(sortString.toUpperCase());

    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);

    if(isTagOnlySearch(q, sort, filterTags)){
      List<String> tags = Arrays.asList(filterTags.split("\\s*,\\s*"));
      template
        .requestAction(context ->
          tagRepository.getRecordIdsByTagName(context.getOkapiData().getTenant(), tags, RecordType.PROVIDER)
            .thenCompose(providerIds ->
              context.getProvidersService().retrieveProviders(convertToLong(providerIds)))
            .thenApply(vendors ->
              Vendors.builder()
                .vendorList(applyPagination(page, count, vendors))
                .totalResults(vendors.getVendorList().size())
                .build()
            )
        );
    }
    else{
      template
        .requestAction(context ->
          context.getProvidersService().retrieveProviders(q, page, count, sort)
        );
    }

    template.executeWithResult(ProviderCollection.class);
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

    final Tags tags = entity.getData().getAttributes().getTags();
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        processUpdateRequest(entity, providerIdLong, context)
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
    String selected = convertToHoldingsSelected(filterSelected);
    parametersValidator.validate(selected, filterType, sort, q);

    Sort nameSort = Sort.valueOf(sort.toUpperCase());

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getPackagesService().retrievePackages(selected, filterType, providerIdLong, q, page, count, nameSort)
      )
      .addErrorMapper(ResourceNotFoundException.class, exception ->
        GetEholdingsProvidersPackagesByProviderIdResponse.respond404WithApplicationVndApiJson(
          ErrorUtil.createError(GET_PROVIDER_NOT_FOUND_MESSAGE)
        ))
      .executeWithResult(PackageCollection.class);
  }

  private String convertToHoldingsSelected(String filterSelected) {
    if (filterSelected == null) {
      return null;
    }
    if (RestConstants.FILTER_SELECTED_MAPPING.containsKey(filterSelected)) {
      return RestConstants.FILTER_SELECTED_MAPPING.get(filterSelected);
    } else {
      throw new ValidationException("Invalid Query Parameter for filter[selected]");
    }
  }

  private void validateSort(String sort) {
    if (!Sort.contains(sort.toUpperCase())) {
      throw new ValidationException("Invalid sort parameter");
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

  private CompletableFuture<VendorById> processUpdateRequest(ProviderPutRequest request, long providerIdLong, RMAPITemplateContext context) {
    if(!providerCanBeUpdated(request)){
      //Return current state of provider without updating it
      return context.getProvidersService().retrieveProvider(providerIdLong);
    }
    return context.getProvidersService().updateProvider(providerIdLong, putRequestConverter.convert(request));
  }

  private boolean providerCanBeUpdated(ProviderPutRequest request) {
    ProviderDataAttributes attributes = request.getData().getAttributes();
    return !Objects.isNull(attributes.getPackagesSelected()) && attributes.getPackagesSelected() != 0;
  }

  private boolean isTagOnlySearch(String q, Sort sort, String filterTags) {
    return Strings.isEmpty(q) && sort.equals(Sort.RELEVANCE) &&
      !Strings.isEmpty(filterTags);
  }

  private List<Long> convertToLong(List<String> strings) {
    return strings.stream()
      .map(Long::valueOf)
      .collect(Collectors.toList());
  }

  private List<Vendor> applyPagination(int page, int count, Vendors vendors) {
    return vendors.getVendorList().stream()
      .sorted(Comparator.comparing(Vendor::getVendorName))
      .skip((long)(page - 1) * count)
      .limit(count)
      .collect(Collectors.toList());
  }
}
