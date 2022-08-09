package org.folio.rmapi.result;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.folio.holdingsiq.model.Title;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.repository.tag.DbTag;
import org.folio.rest.jaxrs.model.Tags;

@Getter
@Setter
public class TitleResult implements Tagable {

  private Title title;
  private boolean includeResource;
  private Tags tags;
  private List<DbTag> resourceTagList;
  private DbAccessType resourceAccessType;

  public TitleResult(Title title, boolean includeResource) {
    this.title = title;
    this.includeResource = includeResource;
  }
}
