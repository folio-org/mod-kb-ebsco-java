package org.folio.rmapi.result;

import java.util.List;
import org.folio.holdingsiq.model.Packages;
import org.folio.repository.packages.DbPackage;

public record PackageCollectionResult(Packages packages, List<DbPackage> dbPackages) { }
