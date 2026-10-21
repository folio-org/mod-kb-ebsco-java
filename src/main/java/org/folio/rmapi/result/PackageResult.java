package org.folio.rmapi.result;

import lombok.Getter;
import lombok.Setter;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.Resources;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.Tags;

@Getter
@Setter
public class PackageResult implements Accessible, Tagable {

  private PackageData packageData;
  private VendorById vendor;
  private Resources resources;
  private Tags tags;
  private AccessType accessType;

  public PackageResult(PackageData packageData) {
    this.packageData = packageData;
  }

  public PackageResult(PackageData packageData, VendorById vendor, Resources resources) {
    this.packageData = packageData;
    this.vendor = vendor;
    this.resources = resources;
  }
}
