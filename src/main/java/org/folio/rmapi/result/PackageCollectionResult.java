package org.folio.rmapi.result;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.folio.holdingsiq.model.Packages;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.repository.packages.DbPackage;

public record PackageCollectionResult(Packages packages, List<DbPackage> dbPackages,
                                      Map<String, DbAccessType> accessTypes) {

  public PackageCollectionResult(Packages packages, List<DbPackage> dbPackages) {
    this(packages, dbPackages, Collections.emptyMap());
  }
}
