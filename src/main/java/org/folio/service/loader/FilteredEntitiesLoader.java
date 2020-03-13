package org.folio.service.loader;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.Titles;
import org.folio.rest.model.filter.AccessTypeFilter;
import org.folio.rest.util.template.RMAPITemplateContext;

public interface FilteredEntitiesLoader {

  CompletableFuture<Packages> fetchPackagesByAccessTypeFilter(AccessTypeFilter accessTypeFilter,
                                                              RMAPITemplateContext context,
                                                              Map<String, String> okapiHeaders);

  CompletableFuture<Titles> fetchTitlesByAccessTypeFilter(AccessTypeFilter accessTypeFilter, RMAPITemplateContext context,
                                                          Map<String, String> okapiHeaders);
}
