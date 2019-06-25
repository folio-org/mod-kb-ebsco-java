package org.folio.service.holdings;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.holdings.DbHolding;
import org.folio.repository.resources.DbResource;
import org.folio.rest.util.template.RMAPITemplateContext;

public interface HoldingsService {
  CompletableFuture<Void> loadHoldings(RMAPITemplateContext context, String tenantId);

  CompletableFuture<List<DbHolding>> getHoldingsByIds(List<DbResource> resourcesResult, String tenant);
}
