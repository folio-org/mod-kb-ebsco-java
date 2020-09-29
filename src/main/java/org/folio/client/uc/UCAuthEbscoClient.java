package org.folio.client.uc;

import java.util.concurrent.CompletableFuture;

import org.folio.client.uc.model.UCAuthToken;

public interface UCAuthEbscoClient {

  CompletableFuture<UCAuthToken> requestToken(String clientId, String clientSecret);
}
