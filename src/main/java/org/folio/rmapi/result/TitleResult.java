package org.folio.rmapi.result;

import java.util.List;

import org.folio.holdingsiq.model.Title;
import org.folio.repository.tag.Tag;
import org.folio.rest.jaxrs.model.Tags;

public class TitleResult {
  private Title title;
  private boolean includeResource;
  private Tags tags;
  private List<Tag> resourceTagList;

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

  public List<Tag> getResourceTagList() {
    return resourceTagList;
  }

  public void setResourceTagList(List<Tag> resourceTagList) {
    this.resourceTagList = resourceTagList;
  }
}
