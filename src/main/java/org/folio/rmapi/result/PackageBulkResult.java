package org.folio.rmapi.result;

import java.util.List;

import lombok.Value;

import org.folio.holdingsiq.model.Packages;

@Value
public class PackageBulkResult {
  Packages packages;
  List<String> failedPackageIds;
}
