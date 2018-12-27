package org.folio.rmapi.result;

import org.folio.rmapi.model.PackageByIdData;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.model.VendorById;

public class ResourceResult {
  private Title title;
  private VendorById vendor;
  private PackageByIdData packageData;
  private boolean includeTitle;

  public ResourceResult(Title title, VendorById vendor, PackageByIdData packageData, boolean includeTitle) {
    this.title = title;
    this.vendor = vendor;
    this.packageData = packageData;
    this.includeTitle = includeTitle;
  }

  public Title getTitle() {
    return title;
  }

  public VendorById getVendor() {
    return vendor;
  }

  public PackageByIdData getPackageData() {
    return packageData;
  }

  public boolean isIncludeTitle() {
    return includeTitle;
  }
}
