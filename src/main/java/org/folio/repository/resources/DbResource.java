package org.folio.repository.resources;

import java.util.List;

import lombok.Builder;
import lombok.Value;

import org.folio.holdingsiq.model.ResourceId;

@Value
@Builder
public class DbResource {
  ResourceId id;
  String credentialsId;
  String name;
  List<String> tags;
}
