package org.folio.service.proxytypes;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.rest.jaxrs.model.ProxyTypes;

public interface ProxyTypesService {

  CompletableFuture<ProxyTypes> findByCredentialsId(String credentialsId, Map<String, String> okapiHeaders);

  CompletableFuture<ProxyTypes> findByUser(Map<String, String> okapiHeaders);
}
