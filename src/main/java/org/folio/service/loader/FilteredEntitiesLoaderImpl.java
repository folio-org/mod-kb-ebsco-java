package org.folio.service.loader;

import static org.folio.db.RowSetUtils.toUUID;
import static org.folio.rest.util.IdParser.getPackageIds;
import static org.folio.rest.util.IdParser.getResourceIds;
import static org.folio.rest.util.IdParser.getTitleIds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
import org.folio.repository.titles.DbTitle;
import org.folio.repository.titles.TitlesRepository;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.model.filter.AccessTypeFilter;
import org.folio.rest.model.filter.TagFilter;
import org.folio.rest.util.IdParser;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.rmapi.PackageServiceImpl;
import org.folio.rmapi.ProvidersServiceImpl;
import org.folio.rmapi.TitlesServiceImpl;
import org.folio.rmapi.result.PackageCollectionResult;
import org.folio.rmapi.result.ResourceCollectionResult;
import org.folio.service.accesstypes.AccessTypeMappingsService;
import org.folio.service.accesstypes.AccessTypesService;
import org.folio.service.holdings.HoldingsService;

@Component
public class FilteredEntitiesLoaderImpl implements FilteredEntitiesLoader {

  @Autowired
  private AccessTypesService accessTypesService;
  @Autowired
  private AccessTypeMappingsService accessTypeMappingsService;
  @Autowired
  private HoldingsService holdingsService;
  @Autowired
  private TagRepository tagRepository;
  @Autowired
  private ProviderRepository providerRepository;
  @Autowired
  private PackageRepository packageRepository;
  @Autowired
  private ResourceRepository resourceRepository;
  @Autowired
  private TitlesRepository titlesRepository;

  @Override
  public CompletableFuture<Packages> fetchPackagesByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                                     RMAPITemplateContext context) {
    AtomicInteger totalCount = new AtomicInteger();
    PackageServiceImpl packagesService = context.getPackagesService();
    return fetchAccessTypeMappings(accessTypeFilter, context, totalCount)
      .thenApply(this::extractPackageIds)
      .thenCompose(packagesService::retrievePackages)
      .thenApply(packages -> packages.toBuilder().totalResults(totalCount.get()).build());
  }

  @Override
  public CompletableFuture<Titles> fetchTitlesByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                                 RMAPITemplateContext context) {
    AtomicInteger totalCount = new AtomicInteger();
    TitlesServiceImpl titlesService = context.getTitlesService();
    return fetchAccessTypeMappings(accessTypeFilter, context, totalCount)
      .thenApply(this::extractTitleIds)
      .thenCompose(titlesService::retrieveTitles)
      .thenApply(titles -> titles.toBuilder().totalResults(totalCount.get()).build());
  }

  @Override
  public CompletableFuture<Vendors> fetchProvidersByTagFilter(TagFilter tagFilter, RMAPITemplateContext context) {
    String tenant = context.getOkapiData().getTenant();
    UUID credentialsId = toUUID(context.getCredentialsId());
    ProvidersServiceImpl providersService = context.getProvidersService();

    return tagRepository.countRecordsByTagFilter(tagFilter, tenant)
      .thenCompose(providerCount -> providerRepository.findIdsByTagFilter(tagFilter, credentialsId, tenant)
        .thenCompose(providersService::retrieveProviders)
        .thenApply(providers -> providers.toBuilder().totalResults(providerCount).build())
      );
  }

  @Override
  public CompletableFuture<PackageCollectionResult> fetchPackagesByTagFilter(TagFilter tagFilter,
                                                                             RMAPITemplateContext context) {
    String tenant = context.getOkapiData().getTenant();
    UUID credentialsId = toUUID(context.getCredentialsId());
    PackageServiceImpl packagesService = context.getPackagesService();

    return tagRepository.countRecordsByTagFilter(tagFilter, tenant)
      .thenCompose(packageCount -> packageRepository.findByTagFilter(tagFilter, credentialsId, tenant)
        .thenCompose(dbPackages -> packagesService.retrievePackages(getPackageIds(dbPackages))
          .thenApply(packages -> toPackageCollectionResult(packages, dbPackages, packageCount))
        )
      );
  }

  @Override
  public CompletableFuture<ResourceCollectionResult> fetchResourcesByTagFilter(TagFilter tagFilter,
                                                                               RMAPITemplateContext context) {
    String tenant = context.getOkapiData().getTenant();
    UUID credentialsId = toUUID(context.getCredentialsId());

    return tagRepository.countRecordsByTagFilter(tagFilter, tenant)
      .thenCompose(resourcesCount -> resourceRepository.findByTagFilter(tagFilter, credentialsId, tenant)
        .thenCompose(dbResources -> holdingsService.getHoldingsByIds(dbResources, context.getCredentialsId(), tenant)
          .thenCompose(dbHoldings -> context.getResourcesService()
            .retrieveResources(getMissingResourceIds(dbHoldings, dbResources), Collections.emptyList())
            .thenApply(resources -> toResourceCollectionResult(resources, dbResources, dbHoldings, resourcesCount))
          )
        )
      );
  }

  @Override
  public CompletableFuture<Titles> fetchTitlesByTagFilter(TagFilter tagFilter, RMAPITemplateContext context) {
    String tenant = context.getOkapiData().getTenant();
    UUID credentialsId = toUUID(context.getCredentialsId());

    return titlesRepository.countTitlesByResourceTags(tagFilter.getTags(), credentialsId, tenant)
      .thenCompose(titlesCount -> titlesRepository.findByTagFilter(tagFilter, credentialsId, tenant)
        .thenCompose(dbTitles -> context.getTitlesService().retrieveTitles(getMissingTitleIds(dbTitles))
          .thenApply(titles -> toTitles(titles, dbTitles, titlesCount))
        )
      );
  }

  private Titles toTitles(Titles titles, List<DbTitle> dbTitles, Integer titlesCount) {
    return titles.toBuilder()
      .titleList(combineTitles(dbTitles, titles.getTitleList()))
      .totalResults(titlesCount)
      .build();
  }

  private ResourceCollectionResult toResourceCollectionResult(Titles titles, List<DbResource> resourcesResult,
                                                              List<DbHoldingInfo> dbHoldings, Integer resourceCount) {
    return new ResourceCollectionResult(titles.toBuilder().totalResults(resourceCount).build(), resourcesResult, dbHoldings);
  }

  private PackageCollectionResult toPackageCollectionResult(Packages packages, List<DbPackage> dbPackages,
                                                            Integer packageCount) {
    return new PackageCollectionResult(packages.toBuilder().totalResults(packageCount).build(), dbPackages);
  }

  private List<Title> combineTitles(List<DbTitle> dbTitles, List<Title> titles) {
    List<Title> resultList = new ArrayList<>(titles);

    dbTitles.stream()
      .map(DbTitle::getTitle)
      .filter(Objects::nonNull)
      .forEach(resultList::add);

    resultList.sort(Comparator.comparing(Title::getTitleName));

    return resultList;
  }

  private List<Long> getMissingTitleIds(List<DbTitle> dbTitles) {
    return dbTitles.stream()
      .filter(title -> Objects.isNull(title.getTitle()))
      .map(DbTitle::getId)
      .collect(Collectors.toList());
  }

  private List<ResourceId> getMissingResourceIds(List<DbHoldingInfo> holdings, List<DbResource> resourcesResult) {
    final List<ResourceId> resourceIds = getTitleIds(resourcesResult);
    resourceIds.removeIf(dbResource -> getResourceIds(holdings).contains(dbResource));
    return resourceIds;
  }

  private CompletableFuture<Collection<AccessTypeMapping>> fetchAccessTypeMappings(AccessTypeFilter accessTypeFilter,
                                                                                   RMAPITemplateContext context,
                                                                                   AtomicInteger totalCount) {
    Map<String, String> okapiHeaders = context.getOkapiData().getOkapiHeaders();
    String credentialsId = context.getCredentialsId();
    RecordType recordType = accessTypeFilter.getRecordType();
    String recordIdPrefix = createRecordIdPrefix(accessTypeFilter);
    return accessTypesService.findByNames(accessTypeFilter.getAccessTypeNames(), credentialsId, okapiHeaders)
      .thenApply(this::extractAccessTypeIds)
      .thenCombine(accessTypeMappingsService.countByRecordPrefix(recordIdPrefix, recordType, credentialsId, okapiHeaders),
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
      .collect(Collectors.toList());
  }

  private List<PackageId> extractPackageIds(Collection<AccessTypeMapping> accessTypeMappings) {
    return accessTypeMappings.stream()
      .map(AccessTypeMapping::getRecordId)
      .map(IdParser::parsePackageId)
      .collect(Collectors.toList());
  }

  private List<Long> extractTitleIds(Collection<AccessTypeMapping> accessTypeMappings) {
    return accessTypeMappings.stream()
      .map(AccessTypeMapping::getRecordId)
      .map(IdParser::parseResourceId)
      .map(ResourceId::getTitleIdPart)
      .collect(Collectors.toList());
  }

}
