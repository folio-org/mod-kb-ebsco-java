package org.folio.rest.impl;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;

import static org.folio.rest.util.ExceptionMappers.error422InputValidationMapper;
import static org.folio.rest.util.IdParser.getPackageIds;
import static org.folio.rest.util.IdParser.parseProviderId;
import static org.folio.rest.util.RequestFiltersUtils.isAccessTypeSearch;
import static org.folio.rest.util.RequestFiltersUtils.isTagsSearch;
import static org.folio.rest.util.RequestFiltersUtils.parseByComma;
import static org.folio.rest.util.RestConstants.JSONAPI;
import static org.folio.rest.util.RestConstants.TAGS_TYPE;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.validation.ValidationException;
import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import org.folio.holdingsiq.model.OkapiData;
import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.Sort;
import org.folio.holdingsiq.model.VendorById;
import org.folio.holdingsiq.model.VendorPut;
import org.folio.holdingsiq.model.Vendors;
import org.folio.holdingsiq.service.exception.ResourceNotFoundException;
import org.folio.holdingsiq.service.validator.PackageParametersValidator;
import org.folio.repository.RecordKey;
import org.folio.repository.RecordType;
import org.folio.repository.packages.PackageInfoInDB;
import org.folio.repository.packages.PackageRepository;
import org.folio.repository.providers.ProviderInfoInDb;
import org.folio.repository.providers.ProviderRepository;
import org.folio.repository.tag.TagRepository;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.Provider;
import org.folio.rest.jaxrs.model.ProviderCollection;
import org.folio.rest.jaxrs.model.ProviderPutDataAttributes;
import org.folio.rest.jaxrs.model.ProviderPutRequest;
import org.folio.rest.jaxrs.model.ProviderTags;
import org.folio.rest.jaxrs.model.ProviderTagsDataAttributes;
import org.folio.rest.jaxrs.model.ProviderTagsItem;
import org.folio.rest.jaxrs.model.ProviderTagsPutRequest;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.resource.EholdingsProviders;
import org.folio.rest.model.filter.AccessTypeFilter;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.RestConstants;
import org.folio.rest.util.template.RMAPITemplate;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.rest.validator.ProviderPutBodyValidator;
import org.folio.rest.validator.ProviderTagsPutBodyValidator;
import org.folio.rmapi.result.PackageCollectionResult;
import org.folio.rmapi.result.VendorResult;
import org.folio.service.loader.FilteredEntitiesLoader;
import org.folio.service.loader.RelatedEntitiesLoader;
import org.folio.spring.SpringContextUtil;

public class EholdingsProvidersImpl implements EholdingsProviders {

  private static final String GET_PROVIDER_NOT_FOUND_MESSAGE = "Provider not found";

  @Autowired
  private Converter<ProviderPutRequest, VendorPut> putRequestConverter;
  @Autowired
  private ProviderPutBodyValidator bodyValidator;
  @Autowired
  private PackageParametersValidator parametersValidator;
  @Autowired
  private ProviderTagsPutBodyValidator providerTagsPutBodyValidator;
  @Autowired
  private RMAPITemplateFactory templateFactory;
  @Autowired
  private TagRepository tagRepository;
  @Autowired
  private ProviderRepository providerRepository;
  @Autowired
  private PackageRepository packageRepository;
  @Autowired
  private RelatedEntitiesLoader relatedEntitiesLoader;
  @Autowired
  private FilteredEntitiesLoader filteredEntitiesLoader;

  public EholdingsProvidersImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }


  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsProviders(String q, String filterTags, String sort, int page, int count,
                                    Map<String, String> okapiHeaders,
                                    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);

    if (isTagsSearch(filterTags, q)) {
      List<String> tags = parseByComma(filterTags);
      template.requestAction(context -> getProvidersByTags(tags, page, count, context));
    } else {
      validateSort(sort);
      validateQuery(q);

      template
        .requestAction(context ->
          context.getProvidersService().retrieveProviders(q, page, count, Sort.valueOf(sort.toUpperCase()))
        );
    }
    template.executeWithResult(ProviderCollection.class);
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsProvidersByProviderId(String providerId, String include, Map<String, String> okapiHeaders,
                                                Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    long providerIdLong = parseProviderId(providerId);

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getProvidersService().retrieveProvider(providerIdLong, include)
          .thenCompose(result ->
            loadTags(result, okapiHeaders)
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
                                                Map<String, String> okapiHeaders,
                                                Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    long providerIdLong = parseProviderId(providerId);

    bodyValidator.validate(entity);

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        processUpdateRequest(entity, providerIdLong, context)
          .thenApply(result -> new VendorResult(result, null)))
      .addErrorMapper(InputValidationException.class, error422InputValidationMapper())
      .executeWithResult(Provider.class);
  }

  @Override
  public void putEholdingsProvidersTagsByProviderId(String providerId, String contentType, ProviderTagsPutRequest entity,
                                                    Map<String, String> okapiHeaders,
                                                    Handler<AsyncResult<Response>> asyncResultHandler,
                                                    Context vertxContext) {
    final Tags tags = entity.getData().getAttributes().getTags();

    completedFuture(null)
      .thenCompose(o -> {
        ProviderTagsDataAttributes attributes = entity.getData().getAttributes();
        providerTagsPutBodyValidator.validate(entity, attributes);
        return updateTags(createDbProvider(providerId, entity.getData().getAttributes()), tags,
          new OkapiData(okapiHeaders).getTenant())
          .thenAccept(ob -> asyncResultHandler.handle(
            Future.succeededFuture(PutEholdingsProvidersTagsByProviderIdResponse.respond200WithApplicationVndApiJson(
              convertToProviderTags(attributes)
            ))));
      })
      .exceptionally(e -> {
        new ErrorHandler()
          .addInputValidation422Mapper()
          .handle(asyncResultHandler, e);
        return null;
      });
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsProvidersPackagesByProviderId(String providerId, String q, String filterTags,
                                                        List<String> filterAccessType, String filterSelected,
                                                        String filterType, String sort, int page, int count,
                                                        Map<String, String> okapiHeaders,
                                                        Handler<AsyncResult<Response>> asyncResultHandler,
                                                        Context vertxContext) {
    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);
    long providerIdLong = parseProviderId(providerId);

    if (isTagsSearch(filterTags, q)) {
      List<String> tags = parseByComma(filterTags);
      template.requestAction(context -> getPackagesByTagsAndProvider(tags, providerId, page, count, context));
    } else if (isAccessTypeSearch(filterAccessType, q, filterSelected, filterTags)) {
      template.requestAction(context -> getPackagesByAccessTypesAndProvider(filterAccessType, providerId, page, count,
        context, okapiHeaders));
    } else {
      String selected = convertToHoldingsSelected(filterSelected);
      parametersValidator.validate(selected, filterType, sort, q);

      Sort nameSort = Sort.valueOf(sort.toUpperCase());

      template
        .requestAction(context ->
          context.getPackagesService().retrievePackages(selected, filterType, providerIdLong, q, page, count, nameSort)
            .thenCompose(packages -> loadTags(packages, context.getOkapiData().getTenant()))
        );
    }
    template
      .addErrorMapper(ResourceNotFoundException.class, exception ->
        GetEholdingsProvidersPackagesByProviderIdResponse.respond404WithApplicationVndApiJson(
          ErrorUtil.createError(GET_PROVIDER_NOT_FOUND_MESSAGE)
        ))
      .executeWithResult(PackageCollection.class);
  }

  private CompletableFuture<Vendors> getProvidersByTags(List<String> tags, int page, int count,
                                                        RMAPITemplateContext context) {
    MutableObject<Integer> totalResults = new MutableObject<>();
    String tenant = context.getOkapiData().getTenant();
    return tagRepository
      .countRecordsByTags(tags, tenant, RecordType.PROVIDER)
      .thenCompose(providerCount -> {
        totalResults.setValue(providerCount);
        return providerRepository.findIdsByTagName(tags, page, count, tenant);
      })
      .thenCompose(providerIds ->
        context.getProvidersService().retrieveProviders(providerIds))
      .thenApply(providers ->
        providers.toBuilder()
          .totalResults(totalResults.getValue())
          .build()
      );
  }

  private CompletableFuture<PackageCollectionResult> getPackagesByTagsAndProvider(List<String> tags, String providerId,
                                                                                  int page, int count,
                                                                                  RMAPITemplateContext context) {
    MutableObject<Integer> totalResults = new MutableObject<>();
    MutableObject<List<PackageInfoInDB>> mutableDbPackages = new MutableObject<>();
    String tenant = context.getOkapiData().getTenant();
    return tagRepository
      .countRecordsByTagsAndPrefix(tags, providerId + "-", tenant, RecordType.PACKAGE)
      .thenCompose(packageCount -> {
        totalResults.setValue(packageCount);
        return packageRepository.findByTagNameAndProvider(tags, providerId, page, count, tenant);
      })
      .thenCompose(dbPackages -> {
        mutableDbPackages.setValue(dbPackages);
        return context.getPackagesService().retrievePackages(getPackageIds(dbPackages));
      })
      .thenApply(packages ->
        new PackageCollectionResult(
          packages.toBuilder()
            .totalResults(totalResults.getValue())
            .build(),
          mutableDbPackages.getValue())
      );
  }

  private CompletableFuture<PackageCollectionResult> getPackagesByAccessTypesAndProvider(List<String> accessTypeNames,
                                                                                         String providerId,
                                                                                         int page, int count,
                                                                                         RMAPITemplateContext context,
                                                                                         Map<String, String> okapiHeaders) {
    AccessTypeFilter accessTypeFilter = new AccessTypeFilter();
    accessTypeFilter.setAccessTypeNames(accessTypeNames);
    accessTypeFilter.setRecordIdPrefix(providerId);
    accessTypeFilter.setRecordType(RecordType.PACKAGE);
    accessTypeFilter.setCount(count);
    accessTypeFilter.setPage(page);
    return filteredEntitiesLoader.fetchPackagesByAccessTypeFilter(accessTypeFilter, context, okapiHeaders)
      .thenApply(packages -> new PackageCollectionResult(packages, emptyList()));
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

  private void validateQuery(String query) {
    if ("".equals(query)) {
      throw new ValidationException("Search parameter cannot be empty");
    }
  }

  private CompletableFuture<VendorResult> loadTags(VendorResult result, Map<String, String> okapiHeaders) {
    RecordKey recordKey = RecordKey.builder()
      .recordId(String.valueOf(result.getVendor().getVendorId()))
      .recordType(RecordType.PROVIDER)
      .build();
    return relatedEntitiesLoader.loadTags(result, recordKey, okapiHeaders)
      .thenApply(aVoid -> result);
  }

  private CompletableFuture<PackageCollectionResult> loadTags(Packages packages, String tenant) {
    return packageRepository.findAllById(getPackageIds(packages), tenant)
      .thenApply(dbPackages -> new PackageCollectionResult(packages, dbPackages));
  }

  private CompletableFuture<Void> updateTags(ProviderInfoInDb provider, Tags tags, String tenant) {
    if (Objects.isNull(tags)) {
      return completedFuture(null);
    } else {
      return updateStoredProvider(provider, tags, tenant)
        .thenCompose(
          o -> tagRepository.updateRecordTags(tenant, provider.getId(), RecordType.PROVIDER, tags.getTagList()))
        .thenApply(updated -> null);
    }
  }

  private ProviderTags convertToProviderTags(ProviderTagsDataAttributes attributes) {
    return new ProviderTags()
      .withData(new ProviderTagsItem()
        .withType(TAGS_TYPE)
        .withAttributes(attributes))
      .withJsonapi(JSONAPI);
  }

  private ProviderInfoInDb createDbProvider(String providerId, ProviderTagsDataAttributes attributes) {
    return ProviderInfoInDb.builder()
      .id(providerId)
      .name(attributes.getName())
      .build();
  }

  private CompletableFuture<Void> updateStoredProvider(ProviderInfoInDb provider, Tags tags, String tenant) {
    if (!tags.getTagList().isEmpty()) {
      return providerRepository.save(provider, tenant);
    }
    return providerRepository.delete(provider.getId(), tenant);
  }

  private CompletableFuture<VendorById> processUpdateRequest(ProviderPutRequest request, long providerIdLong,
                                                             RMAPITemplateContext context) {
    if (!providerCanBeUpdated(request)) {
      //Return current state of provider without updating it
      return context.getProvidersService().retrieveProvider(providerIdLong);
    }
    return context.getProvidersService().updateProvider(providerIdLong, putRequestConverter.convert(request));
  }

  private boolean providerCanBeUpdated(ProviderPutRequest request) {
    ProviderPutDataAttributes attributes = request.getData().getAttributes();
    return !Objects.isNull(attributes.getPackagesSelected()) && attributes.getPackagesSelected() != 0;
  }
}
