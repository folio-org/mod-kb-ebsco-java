package org.folio.service.uc;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.folio.rest.jaxrs.model.ResourceCostPerUse;
import org.folio.rest.jaxrs.model.TitleCostPerUse;

public interface UCCostPerUseService {

  CompletableFuture<ResourceCostPerUse> getResourceCostPerUse(String resourceId, String platform, String fiscalYear,
                                                              Map<String, String> okapiHeaders);

  CompletionStage<TitleCostPerUse> getTitleCostPerUse(String titleId, String platform, String fiscalYear,
                                                      Map<String, String> okapiHeaders);
}
