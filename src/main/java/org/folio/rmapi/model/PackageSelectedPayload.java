package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PackageSelectedPayload {
  @JsonProperty("isSelected")
  private boolean isSelected;

  public PackageSelectedPayload(boolean isSelected) {
    this.isSelected = isSelected;
  }
}
