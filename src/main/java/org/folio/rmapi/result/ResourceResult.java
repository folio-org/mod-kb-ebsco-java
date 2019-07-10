package org.folio.rmapi.result;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.Tags;

public class ResourceResult {
  private Title title;
  private VendorById vendor;
  private PackageByIdData packageData;
  private boolean includeTitle;
  private Tags tags;

  public ResourceResult(Title title, VendorById vendor, PackageByIdData packageData, boolean includeTitle) {
    this.title = title;
    this.vendor = vendor;
    this.packageData = packageData;
    this.includeTitle = includeTitle;
  }

  public ResourceResult(Title title, VendorById vendor, PackageByIdData packageData, boolean includeTitle, Tags tags) {
    this.title = title;
    this.vendor = vendor;
    this.packageData = packageData;
    this.includeTitle = includeTitle;
    this.tags = tags;
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

  public Tags getTags() {
    return tags;
  }

  public void setTags(Tags tags) {
    this.tags = tags;
  }
}
