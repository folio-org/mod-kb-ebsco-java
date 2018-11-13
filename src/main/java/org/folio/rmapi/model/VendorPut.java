package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorPut {
  
  @JsonProperty("vendorToken")
  private VendorPutToken vendorToken;
  
  @JsonProperty("proxy")
  private Proxy proxy;

  public VendorPutToken getVendorToken() {
    return vendorToken;
  }
  public void setVendorPutToken(VendorPutToken vendorToken) {
    this.vendorToken = vendorToken;
  }
  public Proxy getProxy() {
    return proxy;
  }
  public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }
}
