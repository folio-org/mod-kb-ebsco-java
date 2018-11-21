package org.folio.rest.model;

public class ResourceId extends PackageId{

  private long titleIdPart;

  public ResourceId(long providerIdPart, long packageIdPart, long titleIdPart) {
    super(providerIdPart, packageIdPart);
    this.titleIdPart = titleIdPart;
  }
  
  public long getTitleIdPart() {
    return titleIdPart;
  }
}
