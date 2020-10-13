package org.folio.client.uc;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.folio.client.uc.configuration.GetTitlePackageUCConfiguration;
import org.folio.client.uc.configuration.GetTitleUCConfiguration;
import org.folio.client.uc.configuration.UCConfiguration;
import org.folio.client.uc.model.UCCostAnalysis;
import org.folio.client.uc.model.UCTitleCostPerUse;
import org.folio.client.uc.model.UCTitlePackageId;

public interface UCApigeeEbscoClient {

  CompletableFuture<Boolean> verifyCredentials(UCConfiguration configuration);

  CompletableFuture<UCTitleCostPerUse> getTitleCostPerUse(String titleId, String packageId,
                                                          GetTitleUCConfiguration configuration);

  CompletableFuture<Map<String, UCCostAnalysis>> getTitlePackageCostPerUse(Set<UCTitlePackageId> ids,
                                                                           GetTitlePackageUCConfiguration configuration);
}
