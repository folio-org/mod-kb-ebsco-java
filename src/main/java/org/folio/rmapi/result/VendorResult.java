package org.folio.rmapi.result;

import org.folio.rmapi.model.Packages;
import org.folio.rmapi.model.VendorById;

public class VendorResult {
    private VendorById vendor;
    private Packages packages;

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
  }
