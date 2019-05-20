package org.folio.rmapi.result;

import java.util.List;

import org.folio.holdingsiq.model.Packages;
import org.folio.tag.repository.packages.DbPackage;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class PackageCollectionResult {
  private Packages packages;
  private List<DbPackage> dbPackages;
}
