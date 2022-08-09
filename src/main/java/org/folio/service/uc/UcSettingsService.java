package org.folio.service.uc;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.rest.jaxrs.model.UCSettings;
import org.folio.rest.jaxrs.model.UCSettingsKey;
import org.folio.rest.jaxrs.model.UCSettingsPatchRequest;
import org.folio.rest.jaxrs.model.UCSettingsPostRequest;

public interface UcSettingsService {

  CompletableFuture<UCSettings> fetchByUser(boolean includeMetricType, Map<String, String> okapiHeaders);

  CompletableFuture<UCSettings> fetchByCredentialsId(String credentialsId, boolean includeMetricType,
                                                     Map<String, String> okapiHeaders);

  CompletableFuture<UCSettingsKey> fetchKeyByCredentialsId(String credentialsId, Map<String, String> okapiHeaders);

  CompletableFuture<Void> update(String credentialsId, UCSettingsPatchRequest patchRequest,
                                 Map<String, String> okapiHeaders);

  CompletableFuture<UCSettings> save(String credentialsId, UCSettingsPostRequest postRequest,
                                     Map<String, String> okapiHeaders);
}
