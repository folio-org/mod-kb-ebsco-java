package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Vendors {

  @JsonProperty("vendors")
  private List<Vendor> vendorList;

  @JsonProperty("totalResults")
  private Integer totalResults;

  public List<Vendor> getVendorList() {
    return vendorList;
  }

  public void setVendorList(List<Vendor> vendorList) {
    this.vendorList = vendorList;
  }

  public Integer getTotalResults() {
    return totalResults;
  }

  public void setTotalResults(Integer totalResults) {
    this.totalResults = totalResults;
  }
}
