package org.folio.rest.impl;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toMap;

import static org.folio.rest.util.ExceptionMappers.error400NotFoundMapper;
import static org.folio.rest.util.ExceptionMappers.error422InputValidationMapper;
import static org.folio.rest.util.IdParser.getPackageIds;
import static org.folio.rest.util.IdParser.getResourceIds;
import static org.folio.rest.util.IdParser.getTitleIds;
import static org.folio.rest.util.IdParser.packageIdToString;
import static org.folio.rest.util.IdParser.parsePackageId;
import static org.folio.rest.util.RequestFiltersUtils.isAccessTypeSearch;
import static org.folio.rest.util.RequestFiltersUtils.isTagsSearch;
import static org.folio.rest.util.RequestFiltersUtils.parseByComma;
import static org.folio.rest.util.RestConstants.JSONAPI;
import static org.folio.rest.util.RestConstants.TAGS_TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.validation.ValidationException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;

import org.folio.cache.VertxCache;
import org.folio.config.cache.VendorIdCacheKey;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.FilterQuery;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.PackagePost;
import org.folio.holdingsiq.model.PackagePut;
import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.holdingsiq.model.Sort;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.service.exception.ResourceNotFoundException;
import org.folio.holdingsiq.service.validator.PackageParametersValidator;
import org.folio.holdingsiq.service.validator.TitleParametersValidator;
import org.folio.repository.RecordKey;
import org.folio.repository.RecordType;
import org.folio.repository.holdings.HoldingInfoInDB;
import org.folio.repository.packages.PackageInfoInDB;
import org.folio.repository.packages.PackageRepository;
import org.folio.repository.resources.ResourceInfoInDB;
import org.folio.repository.resources.ResourceRepository;
import org.folio.repository.tag.Tag;
import org.folio.repository.tag.TagRepository;
import org.folio.rest.annotations.Validate;
import org.folio.rest.aspect.HandleValidationErrors;
import org.folio.rest.converter.common.ConverterConsts;
import org.folio.rest.converter.packages.PackageRequestConverter;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.Package;
import org.folio.rest.jaxrs.model.PackageBulkFetchCollection;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackagePostBulkFetchRequest;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.PackageTags;
import org.folio.rest.jaxrs.model.PackageTagsDataAttributes;
import org.folio.rest.jaxrs.model.PackageTagsItem;
import org.folio.rest.jaxrs.model.PackageTagsPutRequest;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.resource.EholdingsPackages;
import org.folio.rest.model.filter.AccessTypeFilter;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.RestConstants;
import org.folio.rest.util.template.RMAPITemplate;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.rest.util.template.RMAPITemplateFactory;
import org.folio.rest.validator.CustomPackagePutBodyValidator;
import org.folio.rest.validator.PackagePutBodyValidator;
import org.folio.rest.validator.PackageTagsPutBodyValidator;
import org.folio.rest.validator.PackagesPostBodyValidator;
import org.folio.rmapi.result.PackageResult;
import org.folio.rmapi.result.ResourceCollectionResult;
import org.folio.rmapi.result.TitleCollectionResult;
import org.folio.rmapi.result.TitleResult;
import org.folio.service.accesstypes.AccessTypeMappingsService;
import org.folio.service.accesstypes.AccessTypesService;
import org.folio.service.holdings.HoldingsService;
import org.folio.service.loader.FilteredEntitiesLoader;
import org.folio.service.loader.RelatedEntitiesLoader;
import org.folio.spring.SpringContextUtil;

public class EholdingsPackagesImpl implements EholdingsPackages {

  private static final String PACKAGE_NOT_FOUND_MESSAGE = "Package not found";

  private static final String INVALID_PACKAGE_TITLE = "Package cannot be deleted";
  private static final String INVALID_PACKAGE_DETAILS = "Invalid package";
  private static final String PACKAGE_NOT_UPDATABLE_TITLE = "Package is not updatable";
  private static final String PACKAGE_NOT_UPDATABLE_DETAILS = "Package's 'isCustom' and 'isSelected' are 'false'";

  @Autowired
  private PackageRequestConverter converter;
  @Autowired
  private Converter<PackagePostRequest, PackagePost> packagePostRequestConverter;
  @Autowired
  private PackageParametersValidator packageParametersValidator;
  @Autowired
  private PackagePutBodyValidator packagePutBodyValidator;
  @Autowired
  private CustomPackagePutBodyValidator customPackagePutBodyValidator;
  @Autowired
  private PackageTagsPutBodyValidator packageTagsPutBodyValidator;
  @Autowired
  private PackagesPostBodyValidator packagesPostBodyValidator;
  @Autowired
  private TitleParametersValidator titleParametersValidator;
  @Autowired
  private RMAPITemplateFactory templateFactory;
  @Autowired
  @Qualifier("vendorIdCache")
  private VertxCache<VendorIdCacheKey, Long> vendorIdCache;
  @Autowired
  private TagRepository tagRepository;
  @Autowired
  private PackageRepository packageRepository;
  @Autowired
  private ResourceRepository resourceRepository;
  @Autowired
  private HoldingsService holdingsService;
  @Autowired
  private Converter<Titles, TitleCollectionResult> titleCollectionConverter;
  @Autowired
  private AccessTypesService accessTypesService;
  @Autowired
  private AccessTypeMappingsService accessTypeMappingsService;
  @Autowired
  private RelatedEntitiesLoader relatedEntitiesLoader;
  @Autowired
  private FilteredEntitiesLoader filteredEntitiesLoader;

  public EholdingsPackagesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsPackages(String filterCustom, String q, String filterSelected, String filterType,
                                   String filterTags, List<String> filterAccessType, String sort, int page, int count,
                                   Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
                                   Context vertxContext) {
    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);
    if (isTagsSearch(filterTags, q)) {
      List<String> tags = parseByComma(filterTags);
      template.requestAction(context -> getPackagesByTags(tags, page, count, context));
    } else if (isAccessTypeSearch(filterAccessType, q, filterCustom, filterSelected, filterTags)) {
      AccessTypeFilter accessTypeFilter = new AccessTypeFilter();
      accessTypeFilter.setAccessTypeNames(filterAccessType);
      accessTypeFilter.setRecordType(RecordType.PACKAGE);
      accessTypeFilter.setCount(count);
      accessTypeFilter.setPage(page);
      template.requestAction(context -> filteredEntitiesLoader.fetchPackagesByAccessTypeFilter(accessTypeFilter, context));
    } else {
      if (Objects.nonNull(filterCustom) && !Boolean.parseBoolean(filterCustom)) {
        throw new ValidationException("Invalid Query Parameter for filter[custom]");
      }
      String selected = RestConstants.FILTER_SELECTED_MAPPING.getOrDefault(filterSelected, filterSelected);
      packageParametersValidator.validate(selected, filterType, sort, q);

      boolean isFilterCustom = Boolean.parseBoolean(filterCustom);
      Sort nameSort = Sort.valueOf(sort.toUpperCase());

      template
        .requestAction(context -> {
          if (isFilterCustom) {
            return getVendorId(context)
              .thenCompose(vendorId ->
                context.getPackagesService().retrievePackages(selected, filterType, vendorId, q, page, count, nameSort));
          } else {
            return context.getPackagesService().retrievePackages(selected, filterType, null, q, page, count, nameSort);
          }
        });
    }

    template.executeWithResult(PackageCollection.class);
  }

  @Override
  @HandleValidationErrors
  public void postEholdingsPackages(String contentType, PackagePostRequest entity, Map<String, String> okapiHeaders,
                                    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    packagesPostBodyValidator.validate(entity);
    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);

    PackagePost packagePost = packagePostRequestConverter.convert(entity);
    String accessTypeId = entity.getData().getAttributes().getAccessTypeId();

    if (accessTypeId == null) {
      template.requestAction(context -> postCustomPackage(packagePost, context));
    } else {
      template.requestAction(context -> accessTypesService.findByCredentialsAndAccessTypeId(context.getCredentialsId(),
        accessTypeId, okapiHeaders)
        .thenCompose(accessType -> postCustomPackage(packagePost, context)
          .thenCompose(packageResult -> updateAccessTypeMapping(accessType, packageResult, context))));
    }

    template
      .addErrorMapper(NotFoundException.class, error400NotFoundMapper())
      .executeWithResult(Package.class);
  }

  @Override
  @HandleValidationErrors
  public void getEholdingsPackagesByPackageId(String packageId, String include, Map<String, String> okapiHeaders,
                                              Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PackageId parsedPackageId = parsePackageId(packageId);
    List<String> includedObjects = parseByComma(include);

    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction((context ->
        context.getPackagesService().retrievePackage(parsedPackageId, includedObjects)
          .thenCompose(packageResult -> {
            RecordKey recordKey = RecordKey.builder()
              .recordId(packageIdToString(parsedPackageId))
              .recordType(RecordType.PACKAGE)
              .build();
            return CompletableFuture.allOf(
              relatedEntitiesLoader.loadAccessType(packageResult, recordKey, context),
              relatedEntitiesLoader.loadTags(packageResult, recordKey, context))
              .thenApply(aVoid -> packageResult);
          })
      ))
      .executeWithResult(Package.class);
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsPackagesByPackageId(String packageId, String contentType, PackagePutRequest entity,
                                              Map<String, String> okapiHeaders,
                                              Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if (!isPackageUpdatable(entity)) {
      throw new InputValidationException(PACKAGE_NOT_UPDATABLE_TITLE, PACKAGE_NOT_UPDATABLE_DETAILS);
    } else {
      PackageId parsedPackageId = parsePackageId(packageId);
      templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
        .requestAction(context -> fetchAccessType(entity, context)
          .thenCompose(accessType -> processUpdateRequest(entity, parsedPackageId, context)
            .thenCompose(o -> {
              CompletableFuture<PackageByIdData> future = context.getPackagesService().retrievePackage(parsedPackageId);
              return handleDeletedPackage(future, parsedPackageId, context);
            })
            .thenApply(packageById -> new PackageResult(packageById, null, null))
            .thenCompose(packageResult -> updateAccessTypeMapping(accessType, packageResult, context))
          )
        )
        .addErrorMapper(NotFoundException.class, error400NotFoundMapper())
        .addErrorMapper(InputValidationException.class, error422InputValidationMapper())
        .executeWithResult(Package.class);
    }
  }

  @Override
  @HandleValidationErrors
  public void deleteEholdingsPackagesByPackageId(String packageId, Map<String, String> okapiHeaders,
                                                 Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PackageId parsedPackageId = parsePackageId(packageId);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getPackagesService().retrievePackage(parsedPackageId)
          .thenCompose(packageData -> {
            if (BooleanUtils.isNotTrue(packageData.getIsCustom())) {
              throw new InputValidationException(INVALID_PACKAGE_TITLE, INVALID_PACKAGE_DETAILS);
            }
            return context.getPackagesService().deletePackage(parsedPackageId)
              .thenCompose(aVoid -> deleteAssignedResources(parsedPackageId, context));
          }))
      .execute();
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsPackagesResourcesByPackageId(String packageId, String sort, String filterTags,
                                                       List<String> filterAccessType, String filterSelected,
                                                       String filterType, String filterName, String filterIsxn,
                                                       String filterSubject, String filterPublisher, int page, int count,
                                                       Map<String, String> okapiHeaders,
                                                       Handler<AsyncResult<Response>> asyncResultHandler,
                                                       Context vertxContext) {

    RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);

    if (isTagsSearch(filterTags, filterSelected, filterType, filterName, filterIsxn, filterSubject, filterPublisher)) {
      List<String> tags = parseByComma(filterTags);
      template.requestAction(context -> getTitlesByPackageIdAndTags(packageId, tags, page, count, context));
    } else if (isAccessTypeSearch(filterAccessType, filterTags, filterSelected, filterType, filterName, filterIsxn,
      filterSubject, filterPublisher)) {
      AccessTypeFilter accessTypeFilter = new AccessTypeFilter();
      accessTypeFilter.setAccessTypeNames(filterAccessType);
      accessTypeFilter.setRecordIdPrefix(packageId);
      accessTypeFilter.setRecordType(RecordType.RESOURCE);
      accessTypeFilter.setCount(count);
      accessTypeFilter.setPage(page);
      template.requestAction(context ->
        filteredEntitiesLoader.fetchTitlesByAccessTypeFilter(accessTypeFilter, context)
          .thenApply(titles -> new ResourceCollectionResult(titles, Collections.emptyList(), Collections.emptyList()))
      );
    } else {
      PackageId parsedPackageId = parsePackageId(packageId);

      FilterQuery fq = FilterQuery.builder()
        .selected(RestConstants.FILTER_SELECTED_MAPPING.get(filterSelected))
        .type(filterType)
        .name(filterName).isxn(filterIsxn).subject(filterSubject)
        .publisher(filterPublisher).build();

      titleParametersValidator.validate(fq, sort, true);

      Sort nameSort = Sort.valueOf(sort.toUpperCase());

      template.requestAction(context ->
        context.getTitlesService().retrieveTitles(parsedPackageId.getProviderIdPart(), parsedPackageId.getPackageIdPart(),
          fq, nameSort, page, count)
          .thenApply(titles -> titleCollectionConverter.convert(titles))
          .thenCompose(loadResourceTags(context))
      );
    }

    template.addErrorMapper(ResourceNotFoundException.class, exception ->
      GetEholdingsPackagesResourcesByPackageIdResponse.respond404WithApplicationVndApiJson(
        ErrorUtil.createError(PACKAGE_NOT_FOUND_MESSAGE)))
      .executeWithResult(ResourceCollection.class);
  }

  @Override
  public void putEholdingsPackagesTagsByPackageId(String packageId, String contentType, PackageTagsPutRequest entity,
                                                  Map<String, String> okapiHeaders,
                                                  Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    CompletableFuture.completedFuture(null)
      .thenCompose(o -> {
        packageTagsPutBodyValidator.validate(entity);
        PackageTagsDataAttributes attributes = entity.getData().getAttributes();
        return updateTags(attributes.getTags(), createDbPackage(packageId, attributes),
          new OkapiData(okapiHeaders).getTenant())
          .thenAccept(o2 ->
            asyncResultHandler
              .handle(Future.succeededFuture(PutEholdingsPackagesTagsByPackageIdResponse.respond200WithApplicationVndApiJson(
                convertToPackageTags(attributes)))));
      })
      .exceptionally(e -> {
        new ErrorHandler()
          .addInputValidation422Mapper()
          .handle(asyncResultHandler, e);
        return null;
      });
  }

  @Validate
  @Override
  public void postEholdingsPackagesBulkFetch(String contentType, PackagePostBulkFetchRequest entity,
                                             Map<String, String> okapiHeaders,
                                             Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    final RMAPITemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);

    template.requestAction(context -> context.getPackagesService().retrievePackagesBulk(entity.getPackages()))
      .executeWithResult(PackageBulkFetchCollection.class);
  }

  private CompletableFuture<PackageResult> updateAccessTypeMapping(AccessType accessType,
                                                                   PackageResult packageResult,
                                                                   RMAPITemplateContext context) {
    String recordId = packageResult.getPackageData().getFullPackageId();
    return updateRecordMapping(accessType, recordId, context)
      .thenApply(a -> {
        packageResult.setAccessType(accessType);
        return packageResult;
      });
  }

  private CompletableFuture<Void> updateRecordMapping(AccessType accessType, String recordId,
                                                      RMAPITemplateContext context) {
    return accessTypeMappingsService.update(accessType, recordId, RecordType.PACKAGE, context.getCredentialsId(),
      context.getOkapiData().getOkapiHeaders());
  }

  private CompletableFuture<AccessType> fetchAccessType(PackagePutRequest entity,
                                                        RMAPITemplateContext context) {
    String accessTypeId = entity.getData().getAttributes().getAccessTypeId();
    if (accessTypeId == null) {
      return CompletableFuture.completedFuture(null);
    } else {
      return accessTypesService.findByCredentialsAndAccessTypeId(context.getCredentialsId(), accessTypeId,
        context.getOkapiData().getOkapiHeaders());
    }
  }

  private CompletableFuture<PackageResult> postCustomPackage(PackagePost packagePost, RMAPITemplateContext context) {
    return getVendorId(context)
      .thenCompose(id -> context.getPackagesService().postPackage(packagePost, id))
      .thenApply(packageById -> new PackageResult(packageById, null, null));
  }

  private PackageInfoInDB createDbPackage(String packageId, PackageTagsDataAttributes attributes) {
    return PackageInfoInDB.builder()
      .contentType(ConverterConsts.contentTypes.inverseBidiMap().get(attributes.getContentType()))
      .name(attributes.getName())
      .id(parsePackageId(packageId))
      .build();
  }

  private PackageTags convertToPackageTags(PackageTagsDataAttributes attributes) {
    return new PackageTags()
      .withData(new PackageTagsItem()
        .withType(TAGS_TYPE)
        .withAttributes(attributes))
      .withJsonapi(JSONAPI);
  }

  private Function<TitleCollectionResult, CompletionStage<TitleCollectionResult>> loadResourceTags(
    RMAPITemplateContext context) {
    return titleCollection -> {
      Map<String, TitleResult> resourceIdToTitle = mapResourceIdToTitleResult(titleCollection);

      return tagRepository.findPerRecord(context.getOkapiData().getTenant(),
        new ArrayList<>(resourceIdToTitle.keySet()),
        RecordType.RESOURCE)
        .thenApply(tagMap -> {
          populateResourceTags(resourceIdToTitle, tagMap);
          return titleCollection;
        });
    };
  }

  private void populateResourceTags(Map<String, TitleResult> resourceIdToTitle, Map<String, List<Tag>> tagMap) {
    tagMap.forEach((id, tags) -> {
      TitleResult titleResult = resourceIdToTitle.get(id);
      titleResult.setResourceTagList(tags);
    });
  }

  private Map<String, TitleResult> mapResourceIdToTitleResult(TitleCollectionResult tc) {
    return tc.getTitleResults().stream().collect(toMap(this::getResourceId, Function.identity()));
  }

  private String getResourceId(TitleResult titleResult) {
    CustomerResources resource = titleResult.getTitle().getCustomerResourcesList().get(0);
    return resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId();
  }

  private CompletableFuture<ResourceCollectionResult> getTitlesByPackageIdAndTags(String packageId, List<String> tags,
                                                                                  int page, int count,
                                                                                  RMAPITemplateContext context) {

    MutableObject<Integer> totalResults = new MutableObject<>();
    MutableObject<List<ResourceInfoInDB>> mutableDbTitles = new MutableObject<>();
    MutableObject<List<HoldingInfoInDB>> mutableDbHoldings = new MutableObject<>();
    String tenant = context.getOkapiData().getTenant();

    return tagRepository
      .countRecordsByTagsAndPrefix(tags, packageId + "-", tenant, RecordType.RESOURCE)
      .thenCompose(resourceCount -> {
        totalResults.setValue(resourceCount);
        return resourceRepository.findByTagNameAndPackageId(tags, packageId, page, count, tenant);
      })
      .thenCompose(resourcesResult -> {
        mutableDbTitles.setValue(resourcesResult);
        return holdingsService.getHoldingsByIds(resourcesResult, context.getCredentialsId(), tenant);
      }).thenCompose(dbHoldings -> {
        mutableDbHoldings.setValue(dbHoldings);
        return context.getResourcesService()
          .retrieveResources(getRemainingResourceIds(dbHoldings, mutableDbTitles.getValue()), Collections.emptyList());
      })
      .thenApply(titles -> new ResourceCollectionResult(
        titles.toBuilder().totalResults(totalResults.getValue()).build(),
        mutableDbTitles.getValue(), mutableDbHoldings.getValue())
      );
  }

  private List<ResourceId> getRemainingResourceIds(List<HoldingInfoInDB> holdings, List<ResourceInfoInDB> resourcesResult) {
    final List<ResourceId> resourceIds = getTitleIds(resourcesResult);
    resourceIds.removeIf(dbResource -> getResourceIds(holdings).contains(dbResource));
    return resourceIds;
  }

  private CompletableFuture<Packages> getPackagesByTags(List<String> tags, int page, int count,
                                                        RMAPITemplateContext context) {
    MutableObject<Integer> totalResults = new MutableObject<>();
    String tenant = context.getOkapiData().getTenant();
    return tagRepository
      .countRecordsByTags(tags, tenant, RecordType.PACKAGE)
      .thenCompose(packageCount -> {
        totalResults.setValue(packageCount);
        return packageRepository.findByTagName(tags, page, count, tenant);
      })
      .thenCompose(dbPackages ->
        context.getPackagesService().retrievePackages(getPackageIds(dbPackages)))
      .thenApply(packages ->
        packages.toBuilder()
          .totalResults(totalResults.getValue())
          .build()
      );
  }

  private CompletableFuture<Long> getVendorId(RMAPITemplateContext context) {
    VendorIdCacheKey cacheKey = VendorIdCacheKey.builder()
      .tenant(context.getOkapiData().getTenant())
      .rmapiConfiguration(context.getConfiguration())
      .build();
    Long cachedId = vendorIdCache.getValue(cacheKey);
    if (cachedId != null) {
      return completedFuture(cachedId);
    } else {
      return context.getProvidersService().getVendorId()
        .thenCompose(id -> {
          vendorIdCache.putValue(cacheKey, id);
          return completedFuture(id);
        });
    }
  }

  private CompletableFuture<Void> deleteTags(PackageId packageId, String tenant) {
    return packageRepository.delete(packageId, tenant)
      .thenCompose(o -> tagRepository.deleteRecordTags(tenant, packageIdToString(packageId), RecordType.PACKAGE))
      .thenCompose(aBoolean -> completedFuture(null));
  }

  private CompletableFuture<Void> updateTags(Tags tags, PackageInfoInDB packages, String tenant) {
    if (tags == null) {
      return completedFuture(null);
    } else {
      PackageId id = packages.getId();
      return updateStoredPackage(tags, packages, tenant)
        .thenCompose(
          o -> tagRepository.updateRecordTags(tenant, packageIdToString(id), RecordType.PACKAGE, tags.getTagList()))
        .thenApply(updated -> null);
    }
  }

  private CompletableFuture<Void> updateStoredPackage(Tags tags, PackageInfoInDB packages, String tenant) {
    if (!tags.getTagList().isEmpty()) {
      return packageRepository.save(packages, tenant);
    }
    return packageRepository.delete(packages.getId(), tenant);
  }

  private CompletableFuture<Void> processUpdateRequest(PackagePutRequest entity, PackageId parsedPackageId,
                                                       RMAPITemplateContext context) {
    PackagePut packagePutBody;
    if (BooleanUtils.isTrue(entity.getData().getAttributes().getIsCustom())) {
      customPackagePutBodyValidator.validate(entity);
      packagePutBody = converter.convertToRMAPICustomPackagePutRequest(entity);
    } else {
      packagePutBodyValidator.validate(entity);
      packagePutBody = converter.convertToRMAPIPackagePutRequest(entity);
    }
    return context.getPackagesService().updatePackage(parsedPackageId, packagePutBody);
  }

  private boolean isPackageUpdatable(PackagePutRequest entity) {
    PackagePutDataAttributes packageData = entity.getData().getAttributes();
    if (BooleanUtils.isFalse(packageData.getIsCustom()) &&
      BooleanUtils.isFalse(packageData.getIsSelected())) {
      try {
        packagePutBodyValidator.validate(entity);
      } catch (InputValidationException ex) {
        return false;
      }
    }
    return true;
  }

  /**
   * Delete local package, tags and access type mapping if package was deleted on update
   * (normally this can only happen in case of custom package)
   *
   * @return future with initial result, or exceptionally completed future if deletion of tags failed
   */
  private CompletableFuture<PackageByIdData> handleDeletedPackage(CompletableFuture<PackageByIdData> future,
                                                                  PackageId packageId, RMAPITemplateContext context) {
    CompletableFuture<Void> deleteFuture = new CompletableFuture<>();
    return future.whenComplete((packageById, e) -> {
      if (e instanceof ResourceNotFoundException) {
        deleteAssignedResources(packageId, context).thenAccept(o -> deleteFuture.complete(null));
      } else {
        deleteFuture.complete(null);
      }
    }).thenCombine(deleteFuture, (o, aBoolean) -> future.join());
  }

  private CompletableFuture<Void> deleteAssignedResources(PackageId packageId, RMAPITemplateContext context) {
    CompletableFuture<Void> deleteAccessMapping = updateRecordMapping(null, packageIdToString(packageId), context);
    CompletableFuture<Void> deleteTags = deleteTags(packageId, context.getOkapiData().getTenant());

    return CompletableFuture.allOf(deleteAccessMapping, deleteTags);
  }
}
