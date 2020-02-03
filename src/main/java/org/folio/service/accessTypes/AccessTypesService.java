package org.folio.service.accessTypes;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;

public interface AccessTypesService {

  CompletableFuture<AccessTypeCollectionItem> save(AccessTypeCollectionItem accessType, Map<String, String> okapiHeaders);

}
