package org.folio.client.uc;

import java.util.concurrent.CompletableFuture;

import org.folio.client.uc.model.UCTitleCost;

public interface UCApigeeEbscoClient {

  CompletableFuture<Boolean> verifyCredentials(UCConfiguration configuration);

  CompletableFuture<UCTitleCost> getTitleCost(String titleId, String packageId, GetTitleUCConfiguration configuration);
}
