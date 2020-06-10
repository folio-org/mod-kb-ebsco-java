package org.folio.service.loader;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
import org.folio.holdingsiq.model.Titles;
import org.folio.repository.RecordType;
import org.folio.repository.accesstypes.AccessTypeMapping;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.model.filter.AccessTypeFilter;
import org.folio.rest.util.IdParser;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.rmapi.PackageServiceImpl;
import org.folio.rmapi.TitlesServiceImpl;
import org.folio.service.accesstypes.AccessTypeMappingsService;
import org.folio.service.accesstypes.AccessTypesService;

@Component
public class FilteredEntitiesLoaderImpl implements FilteredEntitiesLoader {

  @Autowired
  private AccessTypesService accessTypesService;
  @Autowired
  private AccessTypeMappingsService accessTypeMappingsService;

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
