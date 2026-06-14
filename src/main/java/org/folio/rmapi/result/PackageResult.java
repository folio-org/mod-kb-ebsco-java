package org.folio.rmapi.result;

import lombok.Getter;
import lombok.Setter;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.Tags;

@Getter
@Setter
public class PackageResult implements Accessible, Tagable {

  private PackageData packageData;
  private VendorById vendor;
  private Titles titles;
  private Tags tags;
  private AccessType accessType;

  public PackageResult(PackageData packageData) {
    this.packageData = packageData;
  }

  public PackageResult(PackageData packageData, VendorById vendor, Titles titles) {
    this.packageData = packageData;
    this.vendor = vendor;
    this.titles = titles;
  }
}
