package org.folio.rmapi.result;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.folio.holdingsiq.model.Resource;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.repository.tag.DbTag;

/**
 * A resource of a package listing together with its locally stored tags and access type.
 */
@Getter
@Setter
public class ResourceItemResult {

  private final Resource resource;
  private List<DbTag> tagList;
  private DbAccessType accessType;

  public ResourceItemResult(Resource resource) {
    this.resource = resource;
  }
}
