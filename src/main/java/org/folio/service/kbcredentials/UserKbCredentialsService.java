package org.folio.service.kbcredentials;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.rest.jaxrs.model.KbCredentials;

public interface UserKbCredentialsService {

  CompletableFuture<KbCredentials> findByUser(Map<String, String> okapiHeaders);
}
