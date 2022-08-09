package org.folio.repository.packages;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import org.folio.holdingsiq.model.PackageId;

@Value
@Builder
public class DbPackage {
  PackageId id;
  UUID credentialsId;
  String name;
  String contentType;
  List<String> tags;
}
