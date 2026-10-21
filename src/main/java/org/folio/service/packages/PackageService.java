package org.folio.service.packages;

import static java.util.stream.Collectors.toMap;
import static org.folio.HttpStatus.SC_NOT_FOUND;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.util.IdParser.packageIdToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.PackagePost;
import org.folio.holdingsiq.model.PackagePut;
import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.Resources;
import org.folio.holdingsiq.service.exception.ResourceNotFoundException;
import org.folio.properties.common.SearchProperties;
import org.folio.repository.RecordKey;
import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.rest.converter.packages.PackageRequestConvertionService;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.PackageTagsDataAttributes;
import org.folio.rest.jaxrs.model.PackageTagsPutRequest;
import org.folio.rest.model.filter.PackageRecordFilter;
import org.folio.rest.model.filter.ResourceFilter;
import org.folio.rest.util.IdParser;
import org.folio.rest.util.template.RmApiTemplateContext;
import org.folio.rest.validator.packages.PackageValidationService;
import org.folio.rmapi.result.PackageResult;
import org.folio.rmapi.result.ResourceItemCollectionResult;
import org.folio.rmapi.result.ResourceItemResult;
import org.folio.service.loader.RelatedEntitiesLoader;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PackageService {

  private static final String INVALID_PACKAGE_TITLE = "Package cannot be deleted";
  private static final String INVALID_PACKAGE_DETAILS = "Invalid package";
  private static final String PACKAGE_IS_CUSTOM_NOT_MATCHED = "Package isCustom not matched";
  private static final String PACKAGE_IS_CUSTOM_NOT_MATCHED_DETAILS = "Package isCustom: %s";

  private final PackageRequestConvertionService requestConvertionService;
  private final PackageValidationService validationService;
  private final PackageAccessTypeService packageAccessTypeService;
  private final PackageTagsService packageTagsService;
  private final CustomProviderIdService customProviderIdService;
  private final RelatedEntitiesLoader relatedEntitiesLoader;
  private final Converter<Resources, ResourceItemCollectionResult> resourceItemCollectionConverter;
  private final SearchProperties searchProperties;

  public CompletableFuture<Packages> retrievePackages(Integer providerId, PackageRecordFilter filter,
                                                      RmApiTemplateContext context) {
    var packageFilter = filter.toClientFilter(searchProperties);
    var pageable = filter.toPageable();
    return providerId == null
           ? context.getPackagesService().retrievePackages(packageFilter, pageable)
           : context.getPackagesService().retrievePackages(providerId, packageFilter, pageable);
  }

  public CompletableFuture<Packages> getCustomProviderIdAndRetrievePackages(PackageRecordFilter filter,
                                                                            RmApiTemplateContext context) {
    return customProviderIdService.getCustomProviderId(context)
      .thenCompose(providerId -> retrievePackages(providerId, filter, context));
  }

  public Function<RmApiTemplateContext, CompletableFuture<?>> retrievePackageTitles(ResourceFilter filter) {
    return context -> {
      var pkgId = filter.parsePackageId();
      return context.getResourcesService()
        .retrieveResources(pkgId.packageIdPart(), filter.toClientFilter(), filter.toPageable())
        .thenCompose(resources -> verifyPackage(resources, pkgId, context))
        .thenApply(resourceItemCollectionConverter::convert)
        .thenCompose(loadResourceTags(context))
        .thenCompose(loadResourceAccessTypes(context));
    };
  }

  // HoldingsIQ v2 addresses a list by its id alone and answers 200 with an empty list for a missing package, so the
  // provider is checked on the returned resources and the 404 for an empty list comes from the package lookup
  private CompletableFuture<Resources> verifyPackage(Resources resources, PackageId packageId,
                                                     RmApiTemplateContext context) {
    if (CollectionUtils.isEmpty(resources.getTitles())) {
      return context.getPackagesService().retrievePackage(packageId).thenApply(packageData -> resources);
    }
    var vendorId = resources.getTitles().getFirst().getVendorId();
    if (vendorId != null && vendorId != packageId.providerIdPart()) {
      return CompletableFuture.failedFuture(new ResourceNotFoundException(
        "Package " + packageId.packageIdPart() + " not found for provider " + packageId.providerIdPart(),
        SC_NOT_FOUND, "Not Found", null, null));
    }
    return CompletableFuture.completedFuture(resources);
  }

  public CompletableFuture<PackageResult> createCustomPackage(PackagePostRequest entity,
                                                              RmApiTemplateContext context) {
    validationService.validateCustomPackagePostRequest(entity);
    PackagePost packagePost = requestConvertionService.convertCustomPackagePostRequest(entity);
    String accessTypeId = entity.getData().getAttributes().getAccessTypeId();

    if (accessTypeId == null) {
      return postCustomPackage(packagePost, context);
    } else {
      return packageAccessTypeService.fetchAccessType(accessTypeId, context)
        .thenCompose(accessType -> postCustomPackage(packagePost, context)
          .thenCompose(packageResult -> packageAccessTypeService.assignAccessType(accessType, packageResult, context)));
    }
  }

  public CompletableFuture<PackageResult> retrievePackageWithRelatedData(PackageId parsedPackageId,
                                                                         List<String> includedObjects,
                                                                         RmApiTemplateContext context) {
    return context.getPackagesService().retrievePackage(parsedPackageId, includedObjects)
      .thenCompose(packageResult -> {
        RecordKey recordKey = RecordKey.builder()
          .recordId(packageIdToString(parsedPackageId))
          .recordType(RecordType.PACKAGE)
          .build();
        return CompletableFuture.allOf(
            relatedEntitiesLoader.loadAccessType(packageResult, recordKey, context),
            relatedEntitiesLoader.loadTags(packageResult, recordKey, context))
          .thenApply(v -> packageResult);
      });
  }

  public CompletableFuture<PackageResult> updatePackage(PackageId parsedPackageId, PackagePutRequest entity,
                                                        RmApiTemplateContext context) {
    String accessTypeId = entity.getData().getAttributes().getAccessTypeId();
    return context.getPackagesService().retrievePackage(parsedPackageId)
      .thenCompose(packageData -> packageAccessTypeService.fetchAccessType(accessTypeId, context)
        .thenCompose(accessType -> processUpdateRequest(entity, packageData, context)
          .thenCompose(voidEntity -> {
            CompletableFuture<PackageData> future = context.getPackagesService().retrievePackage(parsedPackageId);
            return handleDeletedPackage(future, parsedPackageId, context);
          })
          .thenApply(packageById -> new PackageResult(packageById, null, null))
          .thenCompose(packageResult -> packageAccessTypeService.assignAccessType(accessType, packageResult, context))
        )
      );
  }

  public CompletableFuture<Void> deletePackage(PackageId parsedPackageId, RmApiTemplateContext context) {
    var packageIdPart = parsedPackageId.packageIdPart();
    return context.getPackagesService().retrievePackage(parsedPackageId)
      .thenCompose(packageData -> {
        if (BooleanUtils.isNotTrue(packageData.getIsCustom())) {
          throw new InputValidationException(INVALID_PACKAGE_TITLE, INVALID_PACKAGE_DETAILS);
        }
        return context.getPackagesService().deletePackage(packageIdPart)
          .thenCompose(v -> deleteAssignedResources(parsedPackageId, context));
      });
  }

  public CompletableFuture<PackageTagsDataAttributes> updateTagsForPackage(PackageTagsPutRequest entity,
                                                                           UUID credentialsId, String packageId,
                                                                           String tenant) {
    validationService.validatePackageTagsPutRequest(entity);
    PackageTagsDataAttributes attributes = entity.getData().getAttributes();

    return packageTagsService.updatePackageTags(packageId, credentialsId, attributes, tenant)
      .thenApply(v -> attributes);
  }

  private CompletableFuture<PackageResult> postCustomPackage(PackagePost packagePost, RmApiTemplateContext context) {
    return customProviderIdService.getCustomProviderId(context)
      .thenCompose(id -> context.getPackagesService().postPackage(packagePost, id))
      .thenApply(packageById -> new PackageResult(packageById, null, null));
  }

  private CompletableFuture<Void> processUpdateRequest(PackagePutRequest entity, PackageData originalPackage,
                                                       RmApiTemplateContext context) {
    Boolean isEntityCustom = entity.getData().getAttributes().getIsCustom();
    validateIsCustomMatch(originalPackage.getIsCustom(), isEntityCustom);

    PackagePut packagePutBody;
    if (BooleanUtils.isTrue(isEntityCustom)) {
      validationService.validateCustomPackagePutRequest(entity);
      packagePutBody = requestConvertionService.convertCustomPackagePutRequest(entity);
    } else {
      validationService.validateManagedPackagePutRequest(entity);
      packagePutBody = requestConvertionService.convertManagedPackagePutRequest(entity);
    }
    return context.getPackagesService()
      .updatePackage(originalPackage.getPackageId(), packagePutBody);
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
  private CompletableFuture<PackageData> handleDeletedPackage(CompletableFuture<PackageData> future,
                                                              PackageId packageId, RmApiTemplateContext context) {
    CompletableFuture<Void> deleteFuture = new CompletableFuture<>();
    return future.whenComplete((packageById, e) -> {
      var cause = e instanceof CompletionException ? e.getCause() : e;
      if (cause instanceof ResourceNotFoundException) {
        deleteAssignedResources(packageId, context).thenAccept(o -> deleteFuture.complete(null));
      } else {
        deleteFuture.complete(null);
      }
    }).thenCombine(deleteFuture, (o, v) -> future.join());
  }

  private CompletableFuture<Void> deleteAssignedResources(PackageId packageId, RmApiTemplateContext context) {
    CompletableFuture<Void> deleteAccessMapping = packageAccessTypeService.updateRecordMapping(null,
      packageIdToString(packageId), context);
    CompletableFuture<Void> deleteTags = packageTagsService.deletePackageTags(packageId,
      toUUID(context.getCredentialsId()), context.getRequestContext().getTenant());

    return CompletableFuture.allOf(deleteAccessMapping, deleteTags);
  }

  private Function<ResourceItemCollectionResult, CompletionStage<ResourceItemCollectionResult>> loadResourceTags(
    RmApiTemplateContext context) {
    return collection -> {
      Map<String, ResourceItemResult> resourceIdToItem = mapResourceIdToItem(collection);
      return packageTagsService.loadResourceTags(context.getRequestContext().getTenant(), resourceIdToItem)
        .thenApply(v -> collection);
    };
  }

  private Function<ResourceItemCollectionResult, CompletionStage<ResourceItemCollectionResult>>
      loadResourceAccessTypes(RmApiTemplateContext context) {
    return collection -> {
      Map<String, ResourceItemResult> resourceIdToItem = mapResourceIdToItem(collection);
      return relatedEntitiesLoader.loadAccessTypes(new ArrayList<>(resourceIdToItem.keySet()),
          RecordType.RESOURCE, context)
        .thenApply(accessTypeMap -> {
          populateResourceAccessTypes(resourceIdToItem, accessTypeMap);
          return collection;
        });
    };
  }

  private void populateResourceAccessTypes(Map<String, ResourceItemResult> resourceIdToItem,
                                           Map<String, DbAccessType> accessTypeMap) {
    accessTypeMap.forEach((id, accessType) -> {
      if (resourceIdToItem.containsKey(id)) {
        resourceIdToItem.get(id).setAccessType(accessType);
      }
    });
  }

  private Map<String, ResourceItemResult> mapResourceIdToItem(ResourceItemCollectionResult collection) {
    return collection.items().stream()
      .collect(toMap(item -> IdParser.getResourceId(item.getResource()), Function.identity()));
  }
}
