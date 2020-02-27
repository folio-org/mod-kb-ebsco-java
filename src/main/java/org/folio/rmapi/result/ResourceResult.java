package org.folio.rmapi.result;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.model.Tags;

public class ResourceResult implements Accessible, Tagable {

  private Title title;
  private VendorById vendor;
  private PackageByIdData packageData;
  private boolean includeTitle;
  private Tags tags;
  private AccessTypeCollectionItem accessType;

  public ResourceResult(Title title, VendorById vendor, PackageByIdData packageData, boolean includeTitle) {
    this.title = title;
    this.vendor = vendor;
    this.packageData = packageData;
    this.includeTitle = includeTitle;
  }

  public Title getTitle() {
    return title;
  }

  public VendorById getVendor() {
    return vendor;
  }

  public PackageByIdData getPackageData() {
    return packageData;
  }

  public boolean isIncludeTitle() {
    return includeTitle;
  }

  @Override
  public Tags getTags() {
    return tags;
  }

  @Override
  public void setTags(Tags tags) {
    this.tags = tags;
  }

  @Override
  public AccessTypeCollectionItem getAccessType() {
    return accessType;
  }

  @Override
  public void setAccessType(AccessTypeCollectionItem accessType) {
    this.accessType = accessType;
  }
}
