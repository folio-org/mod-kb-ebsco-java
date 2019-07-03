package org.folio.rmapi.result;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Value;

import org.folio.holdingsiq.model.Packages;
import org.folio.repository.packages.PackageInfoInDB;

@Value
@AllArgsConstructor
public class PackageCollectionResult {
  private Packages packages;
  private List<PackageInfoInDB> dbPackages;
}
