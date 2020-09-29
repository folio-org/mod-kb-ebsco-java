package org.folio.client.uc;

import java.util.concurrent.CompletableFuture;

public interface UCApigeeEbscoClient {

  CompletableFuture<Boolean> verifyCredentials(UCConfiguration configuration);
}
