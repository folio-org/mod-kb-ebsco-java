package org.folio.service.uc;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.jaxrs.model.UCSettingsPatchRequest;

public interface UCSettingsService {

  CompletableFuture<UCSettings> fetchByUser(Map<String, String> okapiHeaders);

  CompletableFuture<UCSettings> fetchByCredentialsId(String credentialsId, Map<String, String> okapiHeaders);

  CompletableFuture<Void> update(String credentialsId, UCSettingsPatchRequest patchRequest,
                                 Map<String, String> okapiHeaders);
}
