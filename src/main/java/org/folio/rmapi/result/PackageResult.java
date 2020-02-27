package org.folio.rmapi.result;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.Titles;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.model.Tags;

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

  public PackageByIdData getPackageData() {
    return packageData;
  }

  public VendorById getVendor() {
    return vendor;
  }

  public Titles getTitles() {
    return titles;
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
