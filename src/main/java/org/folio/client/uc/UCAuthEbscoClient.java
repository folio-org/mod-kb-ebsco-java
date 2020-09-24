package org.folio.client.uc;

import java.util.concurrent.CompletableFuture;

public interface UCAuthEbscoClient {

  CompletableFuture<UCAuthToken> requestToken(String clientId, String clientSecret);
}
