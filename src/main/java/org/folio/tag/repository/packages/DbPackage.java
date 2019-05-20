package org.folio.tag.repository.packages;

import java.util.List;

import org.folio.holdingsiq.model.PackageId;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DbPackage {
  private PackageId id;
  private String name;
  private String contentType;
  private List<String> tags;
}
