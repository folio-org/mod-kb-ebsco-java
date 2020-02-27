package org.folio.rmapi.result;

import lombok.Getter;
import lombok.Setter;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.model.Tags;

@Getter
@Setter
public class PackageResult implements Accessible, Tagable {

  private PackageByIdData packageData;
  private VendorById vendor;
  private Titles titles;
  private Tags tags;
  private AccessTypeCollectionItem accessType;

  public PackageResult(PackageByIdData packageData) {
    this.packageData = packageData;
  }

  public PackageResult(PackageByIdData packageData, VendorById vendor, Titles titles) {
    this.packageData = packageData;
    this.vendor = vendor;
    this.titles = titles;
  }
}
