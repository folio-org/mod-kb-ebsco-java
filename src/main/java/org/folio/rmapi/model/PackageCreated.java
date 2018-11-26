package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PackageCreated {

  @JsonProperty("packageId")
  private long packageId;

  public long getPackageId() {
    return packageId;
  }

  public void setPackageId(long packageId) {
    this.packageId = packageId;
  }
}
