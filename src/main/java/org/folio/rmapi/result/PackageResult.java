package org.folio.rmapi.result;

import org.folio.rmapi.model.PackageByIdData;
import org.folio.rmapi.model.Titles;

public class PackageResult {
  private PackageByIdData packageData;
  private Titles titles;

  public PackageResult(PackageByIdData packageData, Titles titles) {
    this.packageData = packageData;
    this.titles = titles;
  }

  public PackageByIdData getPackageData() {
    return packageData;
  }

  public Titles getTitles() {
    return titles;
  }
}
