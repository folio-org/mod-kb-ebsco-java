package org.folio.rmapi.result;

import org.folio.rest.jaxrs.model.AccessType;

public interface Accessible {

  AccessType getAccessType();

  void setAccessType(AccessType accessType);
}
