package org.folio.service.kbcredentials;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.KbCredentialsCollection;
import org.folio.rest.jaxrs.model.KbCredentialsKey;
import org.folio.rest.jaxrs.model.KbCredentialsPatchRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPostRequest;
import org.folio.rest.jaxrs.model.KbCredentialsPutRequest;

public interface KbCredentialsService {

  CompletableFuture<KbCredentials> findByUser(Map<String, String> okapiHeaders);

  CompletableFuture<KbCredentialsCollection> findAll(Map<String, String> okapiHeaders);

  CompletableFuture<KbCredentials> findById(String id, Map<String, String> okapiHeaders);

  CompletableFuture<KbCredentialsKey> findKeyById(String id, Map<String, String> okapiHeaders);

  CompletableFuture<KbCredentials> save(KbCredentialsPostRequest entity, Map<String, String> okapiHeaders);

  CompletableFuture<Void> update(String id, KbCredentialsPutRequest entity, Map<String, String> okapiHeaders);

  CompletableFuture<Void> updatePartially(String id, KbCredentialsPatchRequest entity,
                                          Map<String, String> okapiHeaders);

  CompletableFuture<Void> delete(String id, Map<String, String> okapiHeaders);
}
