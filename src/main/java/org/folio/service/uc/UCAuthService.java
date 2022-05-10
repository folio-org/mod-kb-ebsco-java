package org.folio.service.uc;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.folio.rest.jaxrs.model.UCCredentials;
import org.folio.rest.jaxrs.model.UCCredentialsClientId;
import org.folio.rest.jaxrs.model.UCCredentialsClientSecret;
import org.folio.rest.jaxrs.model.UCCredentialsPresence;

public interface UCAuthService {

  CompletableFuture<String> authenticate(Map<String, String> okapiHeaders);

  CompletableFuture<UCCredentialsClientId> getClientId(Map<String, String> okapiHeaders);

  CompletableFuture<UCCredentialsClientSecret> getClientSecret(Map<String, String> okapiHeaders);

  CompletionStage<UCCredentialsPresence> checkCredentialsPresence(Map<String, String> okapiHeaders);

  CompletionStage<Void> updateCredentials(UCCredentials entity, Map<String, String> okapiHeaders);
}
