package org.folio.rmapi.result;

import org.folio.rmapi.model.Title;

public class TitleResult {
  private Title title;
  private boolean includeResource;

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
}
