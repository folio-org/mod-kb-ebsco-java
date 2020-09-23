package org.folio.client.uc;

import java.util.concurrent.CompletableFuture;

public interface UCAuthServiceClient {

  CompletableFuture<UCAuthToken> requestToken(String clientId, String clientSecret);
}
