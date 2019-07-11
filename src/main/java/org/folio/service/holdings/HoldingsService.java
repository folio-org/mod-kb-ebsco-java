package org.folio.service.holdings;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.repository.holdings.HoldingInfoInDB;
import org.folio.repository.resources.ResourceInfoInDB;
import org.folio.rest.util.template.RMAPITemplateContext;

public interface HoldingsService {

  CompletableFuture<Void> loadHoldings(RMAPITemplateContext context);

  CompletableFuture<List<HoldingInfoInDB>> getHoldingsByIds(List<ResourceInfoInDB> resourcesResult, String tenant);
}
