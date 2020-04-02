package org.folio.service.kbcredentials;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.KbCredentialsCollection;

public interface KbCredentialsService {

  CompletableFuture<KbCredentialsCollection> findAll(Map<String, String> okapiHeaders);
}
