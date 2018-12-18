package org.folio.rmapi.result;

import org.folio.rmapi.model.PackageByIdData;
import org.folio.rmapi.model.Titles;
import org.folio.rmapi.model.VendorById;

public class PackageResult {
  private PackageByIdData packageData;
  private VendorById vendor;
  private Titles titles;

  public PackageResult(PackageByIdData packageData, VendorById vendor, Titles titles) {
    this.packageData = packageData;
    this.vendor = vendor;
    this.titles = titles;
  }

  public PackageByIdData getPackageData() {
    return packageData;
  }

  public VendorById getVendor() {
    return vendor;
  }

  public Titles getTitles() {
    return titles;
  }
}
