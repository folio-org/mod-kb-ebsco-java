package org.folio.rmapi.result;

import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.Tags;

public class VendorResult implements Tagable {

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

  @Override
  public Tags getTags() {
    return tags;
  }

  @Override
  public void setTags(Tags tags) {
    this.tags = tags;
  }
}
