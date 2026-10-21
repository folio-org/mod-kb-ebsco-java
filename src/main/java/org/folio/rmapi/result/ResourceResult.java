package org.folio.rmapi.result;

import lombok.Getter;
import lombok.Setter;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.Resource;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.Tags;

@Getter
@Setter
public class ResourceResult implements Accessible, Tagable {

  private Resource resource;
  private VendorById vendor;
  private PackageData packageData;
  private boolean includeTitle;
  private Tags tags;
  private AccessType accessType;

  public ResourceResult(Resource resource, VendorById vendor, PackageData packageData, boolean includeTitle) {
    this.resource = resource;
    this.vendor = vendor;
    this.packageData = packageData;
    this.includeTitle = includeTitle;
  }
}
