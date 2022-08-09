package org.folio.service.loader;

import java.util.concurrent.CompletableFuture;
import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.model.Vendors;
import org.folio.rest.model.filter.AccessTypeFilter;
import org.folio.rest.model.filter.TagFilter;
import org.folio.rest.util.template.RmApiTemplateContext;
import org.folio.rmapi.result.PackageCollectionResult;
import org.folio.rmapi.result.ResourceCollectionResult;

public interface FilteredEntitiesLoader {

  CompletableFuture<Packages> fetchPackagesByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                              RmApiTemplateContext context);

  CompletableFuture<ResourceCollectionResult> fetchResourcesByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                                               RmApiTemplateContext context);

  CompletableFuture<Titles> fetchTitlesByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                          RmApiTemplateContext context);

  CompletableFuture<Vendors> fetchProvidersByTagFilter(TagFilter tagFilter, RmApiTemplateContext context);

  CompletableFuture<PackageCollectionResult> fetchPackagesByTagFilter(TagFilter tagFilter,
                                                                      RmApiTemplateContext context);

  CompletableFuture<ResourceCollectionResult> fetchResourcesByTagFilter(TagFilter tagFilter,
                                                                        RmApiTemplateContext context);

  CompletableFuture<Titles> fetchTitlesByTagFilter(TagFilter tagFilter, RmApiTemplateContext context);
}
