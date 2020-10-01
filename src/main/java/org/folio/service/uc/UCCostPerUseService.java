package org.folio.service.uc;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.ResourceCostPerUse;

public interface UCCostPerUseService {

  CompletableFuture<ResourceCostPerUse> getResourceCostPerUse(String resourceId, String platform, int fiscalYear,
                                                              Map<String, String> okapiHeaders);
}
