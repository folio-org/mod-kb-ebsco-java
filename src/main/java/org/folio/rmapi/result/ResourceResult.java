package org.folio.rmapi.result;

import lombok.Getter;
import lombok.Setter;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.Tags;

@Getter
@Setter
public class ResourceResult implements Accessible, Tagable {

  private Title title;
  private VendorById vendor;
  private PackageByIdData packageData;
  private boolean includeTitle;
  private Tags tags;
  private AccessType accessType;

  public ResourceResult(Title title, VendorById vendor, PackageByIdData packageData, boolean includeTitle) {
    this.title = title;
    this.vendor = vendor;
    this.packageData = packageData;
    this.includeTitle = includeTitle;
  }
}
