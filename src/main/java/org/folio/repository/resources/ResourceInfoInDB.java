package org.folio.repository.resources;

import java.util.List;

import lombok.Builder;
import lombok.Value;

import org.folio.holdingsiq.model.ResourceId;

@Value
@Builder
public class ResourceInfoInDB {
  private ResourceId id;
  private String name;
  private List<String> tags;
}
