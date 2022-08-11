package org.folio.rest.impl;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toMap;
import static org.folio.common.ListUtils.parseByComma;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.util.ExceptionMappers.error400NotFoundMapper;
import static org.folio.rest.util.ExceptionMappers.error422InputValidationMapper;
import static org.folio.rest.util.IdParser.packageIdToString;
import static org.folio.rest.util.IdParser.parsePackageId;
import static org.folio.rest.util.RestConstants.JSONAPI;
import static org.folio.rest.util.RestConstants.TAGS_TYPE;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.BooleanUtils;
import org.assertj.core.util.Lists;
import org.folio.cache.VertxCache;
import org.folio.config.cache.VendorIdCacheKey;
import org.folio.holdingsiq.model.CustomerResources;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.PackagePost;
import org.folio.holdingsiq.model.PackagePut;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.service.exception.ResourceNotFoundException;
import org.folio.holdingsiq.service.validator.PackageParametersValidator;
import org.folio.properties.common.SearchProperties;
import org.folio.repository.RecordKey;
import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.repository.packages.DbPackage;
import org.folio.repository.packages.PackageRepository;
import org.folio.repository.tag.DbTag;
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
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.PackageTags;
import org.folio.rest.jaxrs.model.PackageTagsDataAttributes;
import org.folio.rest.jaxrs.model.PackageTagsItem;
import org.folio.rest.jaxrs.model.PackageTagsPutRequest;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.jaxrs.resource.EholdingsPackages;
import org.folio.rest.model.filter.Filter;
import org.folio.rest.util.ErrorHandler;
import org.folio.rest.util.ErrorUtil;
import org.folio.rest.util.template.RmApiTemplate;
import org.folio.rest.util.template.RmApiTemplateContext;
import org.folio.rest.util.template.RmApiTemplateFactory;
import org.folio.rest.validator.CustomPackagePutBodyValidator;
import org.folio.rest.validator.PackagePutBodyValidator;
import org.folio.rest.validator.PackageTagsPutBodyValidator;
import org.folio.rest.validator.PackagesPostBodyValidator;
import org.folio.rmapi.result.PackageResult;
import org.folio.rmapi.result.TitleCollectionResult;
import org.folio.rmapi.result.TitleResult;
import org.folio.service.accesstypes.AccessTypeMappingsService;
import org.folio.service.accesstypes.AccessTypesService;
import org.folio.service.kbcredentials.UserKbCredentialsService;
import org.folio.service.loader.FilteredEntitiesLoader;
import org.folio.service.loader.RelatedEntitiesLoader;
import org.folio.spring.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;

public class EholdingsPackagesImpl implements EholdingsPackages {

  private static final String PACKAGE_NOT_FOUND_MESSAGE = "Package not found";

  private static final String INVALID_PACKAGE_TITLE = "Package cannot be deleted";
  private static final String INVALID_PACKAGE_DETAILS = "Invalid package";
  private static final String PACKAGE_IS_CUSTOM_NOT_MATCHED = "Package isCustom not matched";
  private static final String PACKAGE_IS_CUSTOM_NOT_MATCHED_DETAILS = "Package isCustom: %s";

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
  private RmApiTemplateFactory templateFactory;
  @Autowired
  @Qualifier("vendorIdCache")
  private VertxCache<VendorIdCacheKey, Long> vendorIdCache;
  @Autowired
  private TagRepository tagRepository;
  @Autowired
  private PackageRepository packageRepository;
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
  @Autowired
  @Qualifier("securedUserCredentialsService")
  private UserKbCredentialsService userKbCredentialsService;
  @Autowired
  private SearchProperties searchProperties;

  public EholdingsPackagesImpl() {
    SpringContextUtil.autowireDependencies(this, Vertx.currentContext());
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsPackages(String filterCustom, String q, String filterSelected, String filterType,
                                   List<String> filterTags, List<String> filterAccessType, String sort, int page,
                                   int count,
                                   Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
                                   Context vertxContext) {

    Filter filter = Filter.builder()
      .recordType(RecordType.PACKAGE)
      .query(q)
      .filterTags(filterTags)
      .filterCustom(filterCustom)
      .filterAccessType(filterAccessType)
      .filterSelected(filterSelected)
      .filterType(filterType)
      .sort(sort)
      .page(page)
      .count(count)
      .build();

    RmApiTemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);
    if (filter.isTagsFilter()) {
      template.requestAction(
        context -> filteredEntitiesLoader.fetchPackagesByTagFilter(filter.createTagFilter(), context));
    } else if (filter.isAccessTypeFilter()) {
      template.requestAction(context -> filteredEntitiesLoader
        .fetchPackagesByAccessTypeFilter(filter.createAccessTypeFilter(), context)
      );
    } else {
      template
        .requestAction(context -> {
          if (Boolean.TRUE.equals(filter.getFilterCustom())) {
            return getCustomProviderId(context).thenCompose(providerId ->
              context.getPackagesService()
                .retrievePackages(filter.getFilterSelected(), filterType, searchProperties.getPackagesSearchType(),
                  providerId, q, page, count, filter.getSort())
            );
          } else {
            return context.getPackagesService()
              .retrievePackages(filter.getFilterSelected(), filterType, searchProperties.getPackagesSearchType(),
                null, q, page, count, filter.getSort());
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
    RmApiTemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);

    PackagePost packagePost = packagePostRequestConverter.convert(entity);
    String accessTypeId = entity.getData().getAttributes().getAccessTypeId();

    if (accessTypeId == null) {
      template.requestAction(context -> postCustomPackage(packagePost, context));
    } else {
      template.requestAction(context -> accessTypesService.findByCredentialsAndAccessTypeId(context.getCredentialsId(),
          accessTypeId, false, okapiHeaders)
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
      .requestAction(context ->
        context.getPackagesService().retrievePackage(parsedPackageId, includedObjects)
          .thenCompose(packageResult -> {
            RecordKey recordKey = RecordKey.builder()
              .recordId(packageIdToString(parsedPackageId))
              .recordType(RecordType.PACKAGE)
              .build();
            return CompletableFuture.allOf(
                relatedEntitiesLoader.loadAccessType(packageResult, recordKey, context),
                relatedEntitiesLoader.loadTags(packageResult, recordKey, context))
              .thenApply(v -> packageResult);
          })
      )
      .executeWithResult(Package.class);
  }

  @Override
  @HandleValidationErrors
  public void putEholdingsPackagesByPackageId(String packageId, String contentType, PackagePutRequest entity,
                                              Map<String, String> okapiHeaders,
                                              Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PackageId parsedPackageId = parsePackageId(packageId);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context -> context.getPackagesService().retrievePackage(parsedPackageId)
        .thenCompose(packageByIdData -> fetchAccessType(entity, context)
          .thenCompose(accessType -> processUpdateRequest(entity, packageByIdData, context)
            .thenCompose(voidEntity -> {
              CompletableFuture<PackageByIdData> future = context.getPackagesService().retrievePackage(parsedPackageId);
              return handleDeletedPackage(future, parsedPackageId, context);
            })
            .thenApply(packageById -> new PackageResult(packageById, null, null))
            .thenCompose(packageResult -> updateAccessTypeMapping(accessType, packageResult, context))
          )
        )
      )
      .addErrorMapper(NotFoundException.class, error400NotFoundMapper())
      .addErrorMapper(InputValidationException.class, error422InputValidationMapper())
      .executeWithResult(Package.class);
  }

  @Override
  @HandleValidationErrors
  public void deleteEholdingsPackagesByPackageId(String packageId, Map<String, String> okapiHeaders,
                                                 Handler<AsyncResult<Response>> asyncResultHandler,
                                                 Context vertxContext) {
    PackageId parsedPackageId = parsePackageId(packageId);
    templateFactory.createTemplate(okapiHeaders, asyncResultHandler)
      .requestAction(context ->
        context.getPackagesService().retrievePackage(parsedPackageId)
          .thenCompose(packageData -> {
            if (BooleanUtils.isNotTrue(packageData.getIsCustom())) {
              throw new InputValidationException(INVALID_PACKAGE_TITLE, INVALID_PACKAGE_DETAILS);
            }
            return context.getPackagesService().deletePackage(parsedPackageId)
              .thenCompose(v -> deleteAssignedResources(parsedPackageId, context));
          }))
      .execute();
  }

  @Override
  @Validate
  @HandleValidationErrors
  public void getEholdingsPackagesResourcesByPackageId(String packageId, List<String> filterTags,
                                                       List<String> filterAccessType, String filterSelected,
                                                       String filterType, String filterName, String filterIsxn,
                                                       String filterSubject, String filterPublisher, String sort,
                                                       int page,
                                                       int count, Map<String, String> okapiHeaders,
                                                       Handler<AsyncResult<Response>> asyncResultHandler,
                                                       Context vertxContext) {

    Filter filter = Filter.builder()
      .recordType(RecordType.RESOURCE)
      .filterTags(filterTags)
      .packageId(packageId)
      .filterAccessType(filterAccessType)
      .filterSelected(filterSelected)
      .filterType(filterType)
      .filterName(filterName)
      .filterIsxn(filterIsxn)
      .filterSubject(filterSubject)
      .filterPublisher(filterPublisher)
      .sort(sort)
      .page(page)
      .count(count)
      .build();

    RmApiTemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);

    if (filter.isTagsFilter()) {
      template.requestAction(
        context -> filteredEntitiesLoader.fetchResourcesByTagFilter(filter.createTagFilter(), context));
    } else if (filter.isAccessTypeFilter()) {
      template.requestAction(
        context -> filteredEntitiesLoader.fetchResourcesByAccessTypeFilter(filter.createAccessTypeFilter(), context));
    } else {
      template.requestAction(context -> {
        PackageId pkgId = filter.getPackageId();
        long providerIdPart = pkgId.getProviderIdPart();
        long packageIdPart = pkgId.getPackageIdPart();
        return context.getTitlesService()
          .retrieveTitles(providerIdPart, packageIdPart, filter.createFilterQuery(),
            searchProperties.getTitlesSearchType(), filter.getSort(), page, count)
          .thenApply(titles -> titleCollectionConverter.convert(titles))
          .thenCompose(loadResourceTags(context))
          .thenCompose(loadResourceAccessTypes(context));
      });
    }

    template.addErrorMapper(ResourceNotFoundException.class, exception ->
        GetEholdingsPackagesResourcesByPackageIdResponse.respond404WithApplicationVndApiJson(
          ErrorUtil.createError(PACKAGE_NOT_FOUND_MESSAGE)))
      .executeWithResult(ResourceCollection.class);
  }

  @Override
  public void putEholdingsPackagesTagsByPackageId(String packageId, String contentType, PackageTagsPutRequest entity,
                                                  Map<String, String> okapiHeaders,
                                                  Handler<AsyncResult<Response>> asyncResultHandler,
                                                  Context vertxContext) {
    userKbCredentialsService.findByUser(okapiHeaders)
      .thenCompose(creds -> {
        packageTagsPutBodyValidator.validate(entity);

        PackageTagsDataAttributes attributes = entity.getData().getAttributes();

        return updateTags(attributes.getTags(), createDbPackage(packageId, UUID.fromString(creds.getId()), attributes),
          new OkapiData(okapiHeaders).getTenant())
          .thenAccept(o2 ->
            asyncResultHandler
              .handle(
                Future.succeededFuture(PutEholdingsPackagesTagsByPackageIdResponse.respond200WithApplicationVndApiJson(
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
    final RmApiTemplate template = templateFactory.createTemplate(okapiHeaders, asyncResultHandler);

    template.requestAction(context -> context.getPackagesService().retrievePackagesBulk(entity.getPackages()))
      .executeWithResult(PackageBulkFetchCollection.class);
  }

  private CompletableFuture<PackageResult> updateAccessTypeMapping(AccessType accessType,
                                                                   PackageResult packageResult,
                                                                   RmApiTemplateContext context) {
    String recordId = packageResult.getPackageData().getFullPackageId();
    return updateRecordMapping(accessType, recordId, context)
      .thenApply(a -> {
        packageResult.setAccessType(accessType);
        return packageResult;
      });
  }

  private CompletableFuture<Void> updateRecordMapping(AccessType accessType, String recordId,
                                                      RmApiTemplateContext context) {
    return accessTypeMappingsService.update(accessType, recordId, RecordType.PACKAGE, context.getCredentialsId(),
      context.getOkapiData().getHeaders());
  }

  private CompletableFuture<AccessType> fetchAccessType(PackagePutRequest entity,
                                                        RmApiTemplateContext context) {
    String accessTypeId = entity.getData().getAttributes().getAccessTypeId();
    if (accessTypeId == null) {
      return CompletableFuture.completedFuture(null);
    } else {
      return accessTypesService.findByCredentialsAndAccessTypeId(context.getCredentialsId(), accessTypeId, false,
        context.getOkapiData().getHeaders());
    }
  }

  private CompletableFuture<PackageResult> postCustomPackage(PackagePost packagePost, RmApiTemplateContext context) {
    return getCustomProviderId(context)
      .thenCompose(id -> context.getPackagesService().postPackage(packagePost, id))
      .thenApply(packageById -> new PackageResult(packageById, null, null));
  }

  private DbPackage createDbPackage(String packageId, UUID credentialsId, PackageTagsDataAttributes attributes) {
    return DbPackage.builder()
      .id(parsePackageId(packageId))
      .credentialsId(credentialsId)
      .name(attributes.getName())
      .contentType(ConverterConsts.CONTENT_TYPES.inverseBidiMap().get(attributes.getContentType()))
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
    RmApiTemplateContext context) {
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

  private void populateResourceTags(Map<String, TitleResult> resourceIdToTitle, Map<String, List<DbTag>> tagMap) {
    tagMap.forEach((id, tags) -> {
      TitleResult titleResult = resourceIdToTitle.get(id);
      titleResult.setResourceTagList(tags);
    });
  }

  private Function<TitleCollectionResult, CompletionStage<TitleCollectionResult>> loadResourceAccessTypes(
    RmApiTemplateContext context) {
    return titleCollection -> {
      Map<String, TitleResult> resourceIdToAccessType = mapResourceIdToTitleResult(titleCollection);
      String credentialsId = context.getCredentialsId();
      String tenant = context.getOkapiData().getTenant();
      return accessTypesService.findPerRecord(credentialsId, Lists.newArrayList(resourceIdToAccessType.keySet()),
          RecordType.RESOURCE, tenant)
        .thenApply(accessTypeMap -> {
          populateResourceAccessTypes(resourceIdToAccessType, accessTypeMap);
          return titleCollection;
        });
    };
  }

  private void populateResourceAccessTypes(Map<String, TitleResult> resourceIdToTitle,
                                           Map<String, DbAccessType> accessTypeMap) {
    accessTypeMap.forEach((id, accessType) -> {
      if (resourceIdToTitle.containsKey(id)) {
        TitleResult titleResult = resourceIdToTitle.get(id);
        titleResult.setResourceAccessType(accessType);
      }
    });
  }

  private Map<String, TitleResult> mapResourceIdToTitleResult(TitleCollectionResult tc) {
    return tc.getTitleResults().stream().collect(toMap(this::getResourceId, Function.identity()));
  }

  private String getResourceId(TitleResult titleResult) {
    CustomerResources resource = titleResult.getTitle().getCustomerResourcesList().get(0);
    return resource.getVendorId() + "-" + resource.getPackageId() + "-" + resource.getTitleId();
  }

  private CompletableFuture<Long> getCustomProviderId(RmApiTemplateContext context) {
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

  private CompletableFuture<Void> deleteTags(PackageId packageId, RmApiTemplateContext context) {
    UUID credentialsId = toUUID(context.getCredentialsId());
    String tenant = context.getOkapiData().getTenant();

    return packageRepository.delete(packageId, credentialsId, tenant)
      .thenCompose(o -> tagRepository.deleteRecordTags(tenant, packageIdToString(packageId), RecordType.PACKAGE))
      .thenCompose(v -> completedFuture(null));
  }

  private CompletableFuture<Void> updateTags(Tags tags, DbPackage pkg, String tenant) {
    if (tags == null) {
      return completedFuture(null);
    } else {
      PackageId id = pkg.getId();
      return updateStoredPackage(tags, pkg, tenant)
        .thenCompose(
          o -> tagRepository.updateRecordTags(tenant, packageIdToString(id), RecordType.PACKAGE, tags.getTagList()))
        .thenApply(updated -> null);
    }
  }

  private CompletableFuture<Void> updateStoredPackage(Tags tags, DbPackage pkg, String tenant) {
    if (!tags.getTagList().isEmpty()) {
      return packageRepository.save(pkg, tenant);
    }
    return packageRepository.delete(pkg.getId(), pkg.getCredentialsId(), tenant);
  }

  private CompletableFuture<Void> processUpdateRequest(PackagePutRequest entity, PackageByIdData originalPackage,
                                                       RmApiTemplateContext context) {
    Boolean isEntityCustom = entity.getData().getAttributes().getIsCustom();
    validateIsCustomMatch(originalPackage.getIsCustom(), isEntityCustom);

    PackagePut packagePutBody;
    if (BooleanUtils.isTrue(isEntityCustom)) {
      customPackagePutBodyValidator.validate(entity);
      packagePutBody = converter.convertToRmApiCustomPackagePutRequest(entity);
    } else {
      packagePutBodyValidator.validate(entity);
      packagePutBody = converter.convertToRmApiPackagePutRequest(entity);
    }
    return context.getPackagesService()
      .updatePackage(parsePackageId(originalPackage.getFullPackageId()), packagePutBody);
  }

  private void validateIsCustomMatch(Boolean isOriginalCustom, Boolean isUpdatableCustom) {
    if (!isOriginalCustom.equals(isUpdatableCustom)) {
      throw new InputValidationException(PACKAGE_IS_CUSTOM_NOT_MATCHED,
        String.format(PACKAGE_IS_CUSTOM_NOT_MATCHED_DETAILS, isOriginalCustom));
    }
  }

  /**
   * Delete local package, tags and access type mapping if package was deleted on update
   * (normally this can only happen in case of custom package).
   *
   * @return future with initial result, or exceptionally completed future if deletion of tags failed
   */
  private CompletableFuture<PackageByIdData> handleDeletedPackage(CompletableFuture<PackageByIdData> future,
                                                                  PackageId packageId, RmApiTemplateContext context) {
    CompletableFuture<Void> deleteFuture = new CompletableFuture<>();
    return future.whenComplete((packageById, e) -> {
      if (e instanceof ResourceNotFoundException) {
        deleteAssignedResources(packageId, context).thenAccept(o -> deleteFuture.complete(null));
      } else {
        deleteFuture.complete(null);
      }
    }).thenCombine(deleteFuture, (o, v) -> future.join());
  }

  private CompletableFuture<Void> deleteAssignedResources(PackageId packageId, RmApiTemplateContext context) {
    CompletableFuture<Void> deleteAccessMapping = updateRecordMapping(null, packageIdToString(packageId), context);
    CompletableFuture<Void> deleteTags = deleteTags(packageId, context);

    return CompletableFuture.allOf(deleteAccessMapping, deleteTags);
  }
}
