package org.folio.service.rootproxies;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.rest.jaxrs.model.RootProxy;
import org.folio.rest.jaxrs.model.RootProxyPutRequest;

public interface RootProxyService {

  CompletableFuture<RootProxy> findByCredentialsId(String credentialsId, Map<String, String> okapiHeaders);

  CompletableFuture<RootProxy> findByUser(Map<String, String> okapiHeaders);

  CompletableFuture<RootProxy> updateRootProxy(String credentialsId, RootProxyPutRequest entity,
                                               Map<String, String> okapiHeaders);
}
