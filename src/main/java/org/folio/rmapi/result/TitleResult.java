package org.folio.rmapi.result;

import org.folio.holdingsiq.model.Title;
import org.folio.rest.jaxrs.model.Tags;

public class TitleResult {
  private Title title;
  private boolean includeResource;
  private Tags tags;

  public TitleResult(Title title, boolean includeResource) {
    this.title = title;
    this.includeResource = includeResource;
  }

  public Title getTitle() {
    return title;
  }

  public boolean isIncludeResource() {
    return includeResource;
  }

  public Tags getTags() {
    return tags;
  }

  public void setTags(Tags tags) {
    this.tags = tags;
  }
}
