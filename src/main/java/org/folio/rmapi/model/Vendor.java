package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Vendor {
  @JsonProperty("vendorId")
  private int vendorId;
  @JsonProperty("vendorName")
  private String vendorName;
  @JsonProperty("packagesTotal")
  private int packagesTotal;
  @JsonProperty("packagesSelected")
  private int packagesSelected;
  @JsonProperty("isCustomer")
  private boolean isCustomer;
  @JsonProperty("vendorToken")
  private TokenInfo vendorToken;

  public int getVendorId() {
    return vendorId;
  }

  public void setVendorId(int vendorId) {
    this.vendorId = vendorId;
  }

  public String getVendorName() {
    return vendorName;
  }

  public void setVendorName(String vendorName) {
    this.vendorName = vendorName;
  }

  public int getPackagesTotal() {
    return packagesTotal;
  }

  public void setPackagesTotal(int packagesTotal) {
    this.packagesTotal = packagesTotal;
  }

  public int getPackagesSelected() {
    return packagesSelected;
  }

  public void setPackagesSelected(int packagesSelected) {
    this.packagesSelected = packagesSelected;
  }

  public boolean isCustomer() {
    return isCustomer;
  }

  public void setCustomer(boolean customer) {
    isCustomer = customer;
  }

  public TokenInfo getVendorToken() {
    return vendorToken;
  }

  public void setVendorToken(TokenInfo vendorToken) {
    this.vendorToken = vendorToken;
  }
}
