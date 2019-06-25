package org.folio.rest.impl;

import java.util.Arrays;
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
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.Sort;
import org.folio.holdingsiq.model.VendorById;
import org.folio.holdingsiq.model.VendorPut;
import org.folio.holdingsiq.model.Vendors;
import org.folio.holdingsiq.service.exception.ResourceNotFoundException;
import org.folio.holdingsiq.service.validator.PackageParametersValidator;
import org.folio.repository.RecordType;
import org.folio.repository.packages.DbPackage;
import org.folio.repository.packages.PackageRepository;
import org.folio.repository.providers.ProviderRepository;
import org.folio.repository.tag.Tag;
import org.folio.repository.tag.TagRepository;
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
import org.folio.rmapi.result.PackageCollectionResult;
import org.folio.rmapi.result.VendorResult;
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
  private IdParser idParser;
  @Autowired
  private RMAPITemplateFactory templateFactory;
  @Autowired
  private TagRepository tagRepository;
  @Autowired
  private Converter<List<Tag>, Tags> tagsConverter;
  @Autowired
  private ProviderRepository providerRepository;
  @Autowired
  private PackageRepository packageRepository;

  public EholdingsProvidersImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }


  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsProviders(String q, String filterTags, String sort, int page, int count, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);

    if(isTagOnlySearch(q, filterTags)){
      List<String> tags = Arrays.asList(filterTags.split("\\s*,\\s*"));
      template.requestAction(context -> getProvidersByTags(tags, page, count, context));
    }
    else {
      validateSort(sort);
      validateQuery(q);

      template
        .requestAction(context ->
          context.getProvidersService().retrieveProviders(q, page, count, Sort.valueOf(sort.toUpperCase()))
        );
    }
    template
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
  public void getEholdingsProvidersPackagesByProviderId(String providerId, String q, String filterTags, String filterSelected,
                                                        String filterType, String sort, int page, int count,
                                                        Map<String, String> okapiHeaders,
                                                        Handler<AsyncResult<Response>> asyncResultHandler,
                                                        Context vertxContext) {
    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);

    if (isTagOnlySearch(q, filterTags)) {
      List<String> tags = parseTags(filterTags);
      template.requestAction(context -> getPackagesByTagsAndProvider(tags, providerId, page, count, context));
    } else {
      long providerIdLong = idParser.parseProviderId(providerId);
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

  private CompletableFuture<Vendors> getProvidersByTags(List<String> tags, int page, int count, RMAPITemplateContext context) {
    MutableObject<Integer> totalResults = new MutableObject<>();
    String tenant = context.getOkapiData().getTenant();
    return tagRepository
      .countRecordsByTags(tags, tenant, RecordType.PROVIDER)
      .thenCompose(providerCount -> {
        totalResults.setValue(providerCount);
        return providerRepository.getProviderIdsByTagName(tags, page, count, tenant);
      })
      .thenCompose(providerIds ->
        context.getProvidersService().retrieveProviders(providerIds))
      .thenApply(providers ->
        providers.toBuilder()
          .totalResults(totalResults.getValue())
          .build()
      );
  }

  private CompletableFuture<PackageCollectionResult> getPackagesByTagsAndProvider(List<String> tags, String providerId, int page, int count, RMAPITemplateContext context) {
    MutableObject<Integer> totalResults = new MutableObject<>();
    MutableObject<List<DbPackage>> mutableDbPackages = new MutableObject<>();
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

  private CompletableFuture<VendorResult> loadTags(VendorResult result, String tenant) {
    return tagRepository.findByRecord(tenant, String.valueOf(result.getVendor().getVendorId()), RecordType.PROVIDER)
      .thenCompose(tags -> {
        result.setTags(tagsConverter.convert(tags));
        return CompletableFuture.completedFuture(result);
      });
  }

  private CompletableFuture<PackageCollectionResult> loadTags(Packages packages, String tenant) {
    return packageRepository.findAllById(getPackageIds(packages), tenant)
      .thenApply(dbPackages -> new PackageCollectionResult(packages, dbPackages));
  }

  private CompletableFuture<VendorResult> updateTags(VendorById vendorById, String tenant, Tags tags) {
    if (Objects.isNull(tags)) {
      return CompletableFuture.completedFuture(new VendorResult(vendorById, null));
    } else {
      return updateStoredProvider(vendorById, tags, tenant)
        .thenCompose(o -> tagRepository.updateRecordTags(tenant, String.valueOf(vendorById.getVendorId()),
          RecordType.PROVIDER, tags.getTagList()))
        .thenCompose(updated -> {
          VendorResult result = new VendorResult(vendorById, null);
          result.setTags(new Tags().withTagList(tags.getTagList()));
          return CompletableFuture.completedFuture(result);
        });
    }
  }

  private CompletableFuture<Void> updateStoredProvider(VendorById vendorById, Tags tags, String tenant) {
    if (!tags.getTagList().isEmpty()) {
      return providerRepository.saveProvider(vendorById, tenant);
    }
    return providerRepository.deleteProvider(String.valueOf(vendorById.getVendorId()), tenant);
  }

  private CompletableFuture<VendorById> processUpdateRequest(ProviderPutRequest request, long providerIdLong, RMAPITemplateContext context) {
    if (!providerCanBeUpdated(request)) {
      //Return current state of provider without updating it
      return context.getProvidersService().retrieveProvider(providerIdLong);
    }
    return context.getProvidersService().updateProvider(providerIdLong, putRequestConverter.convert(request));
  }

  private boolean providerCanBeUpdated(ProviderPutRequest request) {
    ProviderDataAttributes attributes = request.getData().getAttributes();
    return !Objects.isNull(attributes.getPackagesSelected()) && attributes.getPackagesSelected() != 0;
  }

  private boolean isTagOnlySearch(String q, String filterTags) {
    return Strings.isEmpty(q) && !Strings.isEmpty(filterTags);
  }

  private List<String> parseTags(String filterTags) {
    return Arrays.asList(filterTags.split("\\s*,\\s*"));
  }

  private List<PackageId> getPackageIds(Packages packages) {
    return packages.getPackagesList()
      .stream()
      .map(packageData ->
        PackageId.builder()
          .packageIdPart(packageData.getPackageId())
          .providerIdPart(packageData.getVendorId())
          .build()
      )
      .collect(Collectors.toList());
  }

  private List<PackageId> getPackageIds(List<DbPackage> packageIds) {
    return packageIds.stream().map(DbPackage::getId).collect(Collectors.toList());
  }
}
