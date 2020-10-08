package org.folio.client.uc;

import java.util.concurrent.CompletableFuture;

import org.folio.client.uc.configuration.GetTitleUCConfiguration;
import org.folio.client.uc.configuration.UCConfiguration;
import org.folio.client.uc.model.UCTitleCostPerUse;

public interface UCApigeeEbscoClient {

  CompletableFuture<Boolean> verifyCredentials(UCConfiguration configuration);

  CompletableFuture<UCTitleCostPerUse> getTitleCostPerUse(String titleId, String packageId, GetTitleUCConfiguration configuration);
}
