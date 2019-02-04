package org.folio.rmapi.result;

import org.folio.rest.jaxrs.model.Tags;
import org.folio.rmapi.model.Packages;
import org.folio.rmapi.model.VendorById;

public class VendorResult {
  private VendorById vendor;
  private Packages packages;
  private Tags tags;

  public VendorResult(VendorById vendor, Packages packages) {
    this.vendor = vendor;
    this.packages = packages;
  }

  public VendorById getVendor() {
    return vendor;
  }

  public Packages getPackages() {
    return packages;
  }

  public Tags getTags() {
    return tags;
  }

  public void setTags(Tags tags) {
    this.tags = tags;
  }
}
