package org.folio.client.uc;

import java.util.concurrent.CompletableFuture;
import org.folio.client.uc.model.UcAuthToken;

public interface UcAuthEbscoClient {

  CompletableFuture<UcAuthToken> requestToken(String clientId, String clientSecret);
}
