package org.folio.service.loader;

import static java.util.Collections.emptyList;
import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.tools.utils.TenantTool.tenantId;
import static org.folio.rest.util.IdParser.dbResourcesToIdStrings;
import static org.folio.rest.util.IdParser.getPackageIds;
import static org.folio.rest.util.IdParser.getResourceIds;
import static org.folio.rest.util.IdParser.getTitleIds;
import static org.folio.rest.util.IdParser.resourceIdsToStrings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.db.RowSetUtils;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.ResourceId;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.model.Vendors;
import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypeMapping;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.repository.packages.DbPackage;
import org.folio.repository.packages.PackageRepository;
import org.folio.repository.providers.ProviderRepository;
import org.folio.repository.resources.DbResource;
import org.folio.repository.resources.ResourceRepository;
import org.folio.repository.tag.TagRepository;
import org.folio.repository.titles.TitlesRepository;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.model.filter.AccessTypeFilter;
import org.folio.rest.model.filter.TagFilter;
import org.folio.rest.util.IdParser;
import org.folio.rest.util.template.RmApiTemplateContext;
import org.folio.rmapi.PackageServiceImpl;
import org.folio.rmapi.ProvidersServiceImpl;
import org.folio.rmapi.TitlesServiceImpl;
import org.folio.rmapi.result.PackageCollectionResult;
import org.folio.rmapi.result.ResourceCollectionResult;
import org.folio.service.accesstypes.AccessTypeMappingsService;
import org.folio.service.accesstypes.AccessTypesService;
import org.folio.service.holdings.HoldingsService;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class FilteredEntitiesLoaderImpl implements FilteredEntitiesLoader {

  private final AccessTypesService accessTypesService;
  private final AccessTypeMappingsService accessTypeMappingsService;
  private final HoldingsService holdingsService;
  private final TagRepository tagRepository;
  private final ProviderRepository providerRepository;
  private final PackageRepository packageRepository;
  private final ResourceRepository resourceRepository;
  private final TitlesRepository titlesRepository;

  public FilteredEntitiesLoaderImpl(AccessTypesService accessTypesService,
                                    AccessTypeMappingsService accessTypeMappingsService,
                                    HoldingsService holdingsService, TagRepository tagRepository,
                                    ProviderRepository providerRepository, PackageRepository packageRepository,
                                    ResourceRepository resourceRepository, TitlesRepository titlesRepository) {
    this.accessTypesService = accessTypesService;
    this.accessTypeMappingsService = accessTypeMappingsService;
    this.holdingsService = holdingsService;
    this.tagRepository = tagRepository;
    this.providerRepository = providerRepository;
    this.packageRepository = packageRepository;
    this.resourceRepository = resourceRepository;
    this.titlesRepository = titlesRepository;
  }

  @Override
  public CompletableFuture<Packages> fetchPackagesByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                                     RmApiTemplateContext context) {
    AtomicInteger totalCount = new AtomicInteger();
    PackageServiceImpl packagesService = context.getPackagesService();
    return fetchAccessTypeMappings(accessTypeFilter, context, totalCount)
      .thenApply(this::extractPackageIds)
      .thenCompose(packagesService::retrievePackages)
      .thenApply(packages -> packages.toBuilder().totalResults(totalCount.get()).build());
  }

  @Override
  public CompletableFuture<ResourceCollectionResult> fetchResourcesByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                                                      RmApiTemplateContext context) {
    String tenant = context.getOkapiData().getTenant();
    AtomicInteger totalCount = new AtomicInteger();
    return fetchAccessTypeMappings(accessTypeFilter, context, totalCount)
      .thenApply(this::extractResourceIds)
      .thenCompose(resourceIds -> holdingsService
        .getHoldingsByIds(resourceIdsToStrings(resourceIds), context.getCredentialsId(), tenant)
        .thenCompose(dbHoldings -> context.getResourcesService()
          .retrieveResources(getMissingResourceIds(dbHoldings, resourceIds))
          .thenApply(resources -> toResourceCollectionResult(resources, emptyList(), dbHoldings, totalCount.get()))
        ));
  }

  @Override
  public CompletableFuture<Titles> fetchTitlesByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                                 RmApiTemplateContext context) {
    AtomicInteger totalCount = new AtomicInteger();
    TitlesServiceImpl titlesService = context.getTitlesService();
    return fetchAccessTypeMappings(accessTypeFilter, context, totalCount)
      .thenCompose(accessTypeMappings -> {
        var resourceIds = IdParser.resourceIdsToStrings(extractResourceIds(accessTypeMappings));
        var titleIds = extractTitleIds(accessTypeMappings);

        return titlesService.retrieveTitles(titleIds)
          .thenApply(titles -> titles.toBuilder()
            .titleList(filterResourcesInTitles(titles, resourceIds))
            .totalResults(totalCount.get())
            .build()
          );
      });
  }

  @Override
  public CompletableFuture<Vendors> fetchProvidersByTagFilter(TagFilter tagFilter, RmApiTemplateContext context) {
    String tenant = context.getOkapiData().getTenant();
    UUID credentialsId = toUUID(context.getCredentialsId());
    ProvidersServiceImpl providersService = context.getProvidersService();
    log.debug("fetchProvidersByTagFilter:: by [recordIdPrefix: {}, tenant: {}]",
      tagFilter.getRecordIdPrefix(), tenant);

    return tagRepository.countRecordsByTagFilter(tagFilter, tenant)
      .thenCompose(providerCount -> providerRepository.findIdsByTagFilter(tagFilter, credentialsId, tenant)
        .thenCompose(providersService::retrieveProviders)
        .thenApply(providers -> providers.toBuilder().totalResults(providerCount).build())
      );
  }

  @Override
  public CompletableFuture<PackageCollectionResult> fetchPackagesByTagFilter(TagFilter tagFilter,
                                                                             RmApiTemplateContext context) {
    String tenant = context.getOkapiData().getTenant();
    UUID credentialsId = toUUID(context.getCredentialsId());
    PackageServiceImpl packagesService = context.getPackagesService();
    log.debug("fetchPackagesByTagFilter:: by [recordIdPrefix: {}, tenant: {}]",
      tagFilter.getRecordIdPrefix(), tenant);

    return tagRepository.countRecordsByTagFilter(tagFilter, tenant)
      .thenCompose(packageCount -> packageRepository.findByTagFilter(tagFilter, credentialsId, tenant)
        .thenCompose(dbPackages -> packagesService.retrievePackages(getPackageIds(dbPackages))
          .thenApply(packages -> toPackageCollectionResult(packages, dbPackages, packageCount))
        )
      );
  }

  @Override
  public CompletableFuture<ResourceCollectionResult> fetchResourcesByTagFilter(TagFilter tagFilter,
                                                                               RmApiTemplateContext context) {
    String tenant = context.getOkapiData().getTenant();
    UUID credentialsId = toUUID(context.getCredentialsId());
    log.debug("fetchResourcesByTagFilter:: by [recordIdPrefix: {}. tenant: {}]",
      tagFilter.getRecordIdPrefix(), tenant);

    return tagRepository.countRecordsByTagFilter(tagFilter, tenant)
      .thenCompose(resourcesCount -> resourceRepository.findByTagFilter(tagFilter, credentialsId, tenant)
        .thenCompose(dbResources -> {
            List<String> ids = dbResourcesToIdStrings(dbResources);
            return holdingsService.getHoldingsByIds(ids, context.getCredentialsId(), tenant)
              .thenCompose(dbHoldings -> context.getResourcesService()
                .retrieveResources(getMissingResourceIds(dbHoldings, getTitleIds(dbResources)))
                .thenApply(resources -> toResourceCollectionResult(resources, dbResources, dbHoldings, resourcesCount))
              );
          }
        )
      );
  }

  @Override
  public CompletableFuture<Titles> fetchTitlesByTagFilter(TagFilter tagFilter, RmApiTemplateContext context) {
    String tenant = context.getOkapiData().getTenant();
    UUID credentialsId = toUUID(context.getCredentialsId());
    log.debug("fetchTitlesByTagFilter:: by [recordIdPrefix: {}. tenant: {}]",
      tagFilter.getRecordIdPrefix(), tenant);

    return titlesRepository.countTitlesByResourceTags(tagFilter.getTags(), credentialsId, tenant)
      .thenCompose(titlesCount -> resourceRepository.findByTagFilter(tagFilter, credentialsId, tenant)
        .thenCompose(dbResources -> {
            var resourceIds = dbResourcesToIdStrings(dbResources);
            var titleIds = extractTitleIds(dbResources);
            return context.getTitlesService().retrieveTitles(titleIds)
              .thenApply(titles -> titles.toBuilder()
                .titleList(filterResourcesInTitles(titles, resourceIds))
                .totalResults(titlesCount)
                .build()
              );
          }
        )
      );
  }

  private List<Title> filterResourcesInTitles(Titles titles, List<String> resourceIds) {
    return titles.getTitleList().stream()
      .map(title -> {
        var filteredCustomerResources = title.getCustomerResourcesList().stream()
          .filter(resource -> resourceIds.contains(IdParser.getResourceId(resource)))
          .collect(Collectors.toCollection(ArrayList::new));
        return title.toBuilder()
          .customerResourcesList(filteredCustomerResources)
          .build();
      })
      .collect(Collectors.toCollection(ArrayList::new));
  }

  private ResourceCollectionResult toResourceCollectionResult(Titles titles, List<DbResource> resourcesResult,
                                                              List<DbHoldingInfo> dbHoldings, Integer resourceCount) {
    return new ResourceCollectionResult(titles.toBuilder().totalResults(resourceCount).build(), resourcesResult,
      dbHoldings);
  }

  private PackageCollectionResult toPackageCollectionResult(Packages packages, List<DbPackage> dbPackages,
                                                            Integer packageCount) {
    return new PackageCollectionResult(packages.toBuilder().totalResults(packageCount).build(), dbPackages);
  }

  private List<ResourceId> getMissingResourceIds(List<DbHoldingInfo> holdings, List<ResourceId> resourceIds) {
    List<ResourceId> holdingsIds = getResourceIds(holdings);
    return org.apache.commons.collections4.ListUtils.select(resourceIds,
      resourceId -> !holdingsIds.contains(resourceId));
  }

  private CompletableFuture<Collection<AccessTypeMapping>> fetchAccessTypeMappings(AccessTypeFilter accessTypeFilter,
                                                                                   RmApiTemplateContext context,
                                                                                   AtomicInteger totalCount) {
    Map<String, String> okapiHeaders = context.getOkapiData().getHeaders();
    String credentialsId = context.getCredentialsId();
    RecordType recordType = accessTypeFilter.getRecordType();
    String recordIdPrefix = createRecordIdPrefix(accessTypeFilter);

    log.info("Attempts to find & collect accessTypes [recordType: {}, recordIdPrefix: {}, tenant: {}]",
      recordType, recordIdPrefix, tenantId(okapiHeaders));

    return accessTypesService.findByNames(accessTypeFilter.getAccessTypeNames(), credentialsId, okapiHeaders)
      .thenApply(this::extractAccessTypeIds)
      .thenCombine(
        accessTypeMappingsService.countByRecordPrefix(recordIdPrefix, recordType, credentialsId, okapiHeaders),
        (accessTypeIds, mappingCount) -> {
          accessTypeFilter.setAccessTypeIds(accessTypeIds);
          accessTypeFilter.setRecordIdPrefix(recordIdPrefix);
          accessTypeIds.forEach(id -> totalCount.getAndAdd(mappingCount.getOrDefault(id, 0)));
          return accessTypeFilter;
        })
      .thenCompose(filter -> accessTypeMappingsService.findByAccessTypeFilter(filter, okapiHeaders));
  }

  private String createRecordIdPrefix(AccessTypeFilter accessTypeFilter) {
    String recordIdPrefix = accessTypeFilter.getRecordIdPrefix();
    return StringUtils.isBlank(recordIdPrefix) ? "" : StringUtils.appendIfMissing(recordIdPrefix, "-");
  }

  private List<UUID> extractAccessTypeIds(AccessTypeCollection accessTypeCollection) {
    return accessTypeCollection.getData()
      .stream()
      .map(AccessType::getId)
      .map(RowSetUtils::toUUID)
      .collect(Collectors.toCollection(ArrayList::new));
  }

  private List<PackageId> extractPackageIds(Collection<AccessTypeMapping> accessTypeMappings) {
    return accessTypeMappings.stream()
      .map(AccessTypeMapping::getRecordId)
      .map(IdParser::parsePackageId)
      .collect(Collectors.toCollection(ArrayList::new));
  }

  private List<Long> extractTitleIds(Collection<AccessTypeMapping> accessTypeMappings) {
    return accessTypeMappings.stream()
      .map(AccessTypeMapping::getRecordId)
      .map(IdParser::parseResourceId)
      .map(ResourceId::getTitleIdPart)
      .distinct()
      .collect(Collectors.toCollection(ArrayList::new));
  }

  private List<Long> extractTitleIds(List<DbResource> dbResources) {
    return dbResources.stream()
      .map(DbResource::getId)
      .map(ResourceId::getTitleIdPart)
      .distinct()
      .collect(Collectors.toCollection(ArrayList::new));
  }

  private List<ResourceId> extractResourceIds(Collection<AccessTypeMapping> accessTypeMappings) {
    return accessTypeMappings.stream()
      .map(AccessTypeMapping::getRecordId)
      .map(IdParser::parseResourceId)
      .collect(Collectors.toCollection(ArrayList::new));
  }
}
