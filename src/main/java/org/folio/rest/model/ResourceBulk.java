package org.folio.rest.model;

import java.util.List;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import org.folio.holdingsiq.model.ResourceId;

@Value
@Builder(toBuilder = true)
public class ResourceBulk {

  @Singular
  List<ResourceId> resourceIds;
  @Singular
  List<String> faults;
}
