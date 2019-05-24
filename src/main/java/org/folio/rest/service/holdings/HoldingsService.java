package org.folio.rest.service.holdings;

import java.util.concurrent.CompletableFuture;

import org.folio.rest.util.template.RMAPITemplateContext;

public interface HoldingsService {
  CompletableFuture<Void> loadHoldings(RMAPITemplateContext context, String tenantId);
}
