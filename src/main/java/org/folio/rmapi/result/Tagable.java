package org.folio.rmapi.result;

import org.folio.rest.jaxrs.model.Tags;

public interface Tagable {

  Tags getTags();

  void setTags(Tags tags);
}
