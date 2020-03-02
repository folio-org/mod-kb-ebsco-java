package org.folio.rmapi.result;

import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;

public interface Accessible {

  AccessTypeCollectionItem getAccessType();

  void setAccessType(AccessTypeCollectionItem accessType);
}
