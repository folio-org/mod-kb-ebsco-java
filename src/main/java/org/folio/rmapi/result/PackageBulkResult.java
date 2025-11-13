package org.folio.rmapi.result;

import java.util.List;
import org.folio.holdingsiq.model.Packages;

public record PackageBulkResult(Packages packages, List<String> failedPackageIds) { }
