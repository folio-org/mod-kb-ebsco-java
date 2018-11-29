package org.folio.rmapi.result;

import org.folio.rmapi.model.Title;
import org.folio.rmapi.model.VendorById;

public class ResourceResult {
    private Title title;
    private VendorById vendor;

    public ResourceResult(Title title, VendorById vendor) {
      this.title = title;
      this.vendor = vendor;
    }

    public Title getTitle() {
      return title;
    }

    public VendorById getVendor() {
      return vendor;
    }
  }
