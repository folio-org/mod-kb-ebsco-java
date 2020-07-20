package org.folio.service.loader;

import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.model.Vendors;
import org.folio.rest.model.filter.AccessTypeFilter;
import org.folio.rest.model.filter.TagFilter;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.rmapi.result.PackageCollectionResult;
import org.folio.rmapi.result.ResourceCollectionResult;

public interface FilteredEntitiesLoader {

  CompletableFuture<Packages> fetchPackagesByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                              RMAPITemplateContext context);

  CompletableFuture<Titles> fetchTitlesByAccessTypeFilter(AccessTypeFilter accessTypeFilter, RMAPITemplateContext context);

  CompletableFuture<Vendors> fetchProvidersByTagFilter(TagFilter tagFilter, RMAPITemplateContext context);

  CompletableFuture<PackageCollectionResult> fetchPackagesByTagFilter(TagFilter tagFilter, RMAPITemplateContext context);

  CompletableFuture<ResourceCollectionResult> fetchResourcesByTagFilter(TagFilter tagFilter, RMAPITemplateContext context);

  CompletableFuture<Titles> fetchTitlesByTagFilter(TagFilter tagFilter, RMAPITemplateContext context);
}
