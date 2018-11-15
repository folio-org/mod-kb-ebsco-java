package org.folio.rest.model;

public class PackageId {
  private long providerIdPart;
  private long packageIdPart;

  public PackageId(long providerIdPart, long packageIdPart) {
    this.providerIdPart = providerIdPart;
    this.packageIdPart = packageIdPart;
  }

  public long getProviderIdPart() {
    return providerIdPart;
  }

  public long getPackageIdPart() {
    return packageIdPart;
  }
}
