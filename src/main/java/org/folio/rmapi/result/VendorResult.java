package org.folio.rmapi.result;

import lombok.Getter;
import lombok.Setter;
import org.folio.holdingsiq.model.Packages;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.Tags;

@Getter
@Setter
public class VendorResult implements Tagable {

  private VendorById vendor;
  private Packages packages;
  private Tags tags;

  public VendorResult(VendorById vendor, Packages packages) {
    this.vendor = vendor;
    this.packages = packages;
  }
}
