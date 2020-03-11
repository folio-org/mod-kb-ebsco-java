package org.folio.rmapi;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.validation.ValidationException;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.folio.cache.VertxCache;
import org.folio.common.FutureUtils;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.service.PackagesHoldingsIQService;
import org.folio.holdingsiq.service.impl.ResourcesHoldingsIQServiceImpl;
import org.folio.rest.model.ResourceBulk;
import org.folio.rest.parser.IdParser;
import org.folio.rmapi.cache.ResourceCacheKey;
import org.folio.rmapi.result.ResourceBulkResult;
import org.folio.rmapi.result.ResourceResult;
import org.folio.rmapi.result.VendorResult;

public class ResourcesServiceImpl extends ResourcesHoldingsIQServiceImpl {

  private static final Logger LOG = LoggerFactory.getLogger(ResourcesServiceImpl.class);
  private static final String INCLUDE_PROVIDER_VALUE = "provider";
  private static final String INCLUDE_PACKAGE_VALUE = "package";

  private ProvidersServiceImpl providerService;
  private PackagesHoldingsIQService packagesService;
  private VertxCache<ResourceCacheKey, Title> resourceCache;
  private Configuration configuration;
  private String tenantId;

  public ResourcesServiceImpl(Configuration config, Vertx vertx, String tenantId,
                              ProvidersServiceImpl providerService, PackagesHoldingsIQService packagesService,
                              VertxCache<ResourceCacheKey, Title> resourceCache) {
    super(config, vertx);
    this.providerService = providerService;
    this.packagesService = packagesService;
    this.configuration = config;
    this.tenantId = tenantId;
    this.resourceCache = resourceCache;
  }

  public CompletableFuture<ResourceResult> retrieveResource(ResourceId resourceId, List<String> includes) {
    return retrieveResource(resourceId, includes, false);
  }

  public CompletableFuture<ResourceResult> retrieveResource(ResourceId resourceId, List<String> includes, boolean useCache) {
    CompletableFuture<Title> titleFuture;
    CompletableFuture<PackageByIdData> packageFuture;
    CompletableFuture<VendorResult> vendorFuture;

    if(useCache){
      titleFuture = retrieveResourceWithCache(resourceId, tenantId, configuration);
    }
    else {
      titleFuture = super.retrieveResource(resourceId);
    }
    if (includes.contains(INCLUDE_PROVIDER_VALUE)) {
      vendorFuture = providerService.retrieveProvider(resourceId.getProviderIdPart(), "");
    } else {
      vendorFuture = completedFuture(new VendorResult(null, null));
    }
    if (includes.contains(INCLUDE_PACKAGE_VALUE)) {
      PackageId id = PackageId.builder()
        .providerIdPart(resourceId.getProviderIdPart())
        .packageIdPart(resourceId.getPackageIdPart()).build();
      packageFuture = packagesService.retrievePackage(id);
    } else {
      packageFuture = completedFuture(null);
    }
    boolean includeTitle = includes.contains("title");

    return CompletableFuture.allOf(titleFuture, vendorFuture, packageFuture)
      .thenCompose(o ->
        completedFuture(new ResourceResult(titleFuture.join(), vendorFuture.join().getVendor(), packageFuture.join(), includeTitle)));
  }

  private CompletableFuture<Title> retrieveResourceWithCache(ResourceId resourceId, String tenantId, Configuration configuration) {
    ResourceCacheKey cacheKey = ResourceCacheKey.builder()
      .resourceId(resourceId)
      .tenant(tenantId)
      .rmapiConfiguration(configuration)
      .build();
    return resourceCache.getValueOrLoad(cacheKey, () -> retrieveResource(resourceId));
  }

  public CompletableFuture<Titles> retrieveResources(List<ResourceId> resourceIds, List<String> includes) {
    Set<CompletableFuture<ResourceResult>> futures = resourceIds.stream()
      .map(id -> retrieveResource(id, includes, true))
      .collect(Collectors.toSet());

    return FutureUtils.allOfSucceeded(futures, throwable -> LOG.warn(throwable.getMessage(), throwable))
      .thenApply(this::mapToResources);
  }

  public CompletableFuture<ResourceBulkResult> retrieveResourcesBulk(ResourceBulk resourceBulk, List<String> includes) {
    List<String> failed = new ArrayList<>(resourceBulk.getFaults());
    Set<CompletableFuture<ResourceResult>> futures = resourceBulk.getResourceIds().stream()
      .map(id -> retrieveResource(id, includes, true)
        .exceptionally(throwable -> {
          failed.add(id.getProviderIdPart() + "-" + id.getPackageIdPart() + "-" + id.getTitleIdPart());
          return new ResourceResult(null, null, null, false);
        }))
      .collect(Collectors.toSet());

    return FutureUtils.allOfSucceeded(futures, throwable -> LOG.warn(throwable.getMessage(), throwable))
      .thenApply(resourceFutures -> mapToResources(resourceFutures, failed));
  }

  private ResourceBulkResult mapToResources(List<ResourceResult> resourceFutures, List<String> failed) {
    List<Title> titlesList = resourceFutures.stream()
      .filter(title -> Objects.nonNull(title.getTitle()))
      .map(ResourceResult::getTitle)
      .sorted(Comparator.comparing(Title::getTitleName))
      .collect(Collectors.toList());
    return new ResourceBulkResult(titlesList, failed);
  }

  private Titles mapToResources(List<ResourceResult> resourceFutures) {
    List<Title> titlesList = resourceFutures.stream()
      .map(ResourceResult::getTitle)
      .sorted(Comparator.comparing(Title::getTitleName))
      .collect(Collectors.toList());
    return Titles.builder()
      .titleList(titlesList)
      .build();
  }

  public ResourceBulk parseToResourceId(Set<String> packagesSet, IdParser idParser) {
    final ResourceBulk.ResourceBulkBuilder builder = ResourceBulk.builder();

    packagesSet.forEach(resourceId -> {
      try {
        builder.resourceId(idParser.parseResourceId(resourceId));
      } catch (ValidationException exception){
        builder.fault(resourceId);
      }
    });
    return builder.build();
  }
}
