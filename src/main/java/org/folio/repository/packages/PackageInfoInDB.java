package org.folio.repository.packages;

import java.util.List;

import lombok.Builder;
import lombok.Value;

import org.folio.holdingsiq.model.PackageId;

@Value
@Builder
public class PackageInfoInDB {
  private PackageId id;
  private String credentialsId;
  private String name;
  private String contentType;
  private List<String> tags;
}
