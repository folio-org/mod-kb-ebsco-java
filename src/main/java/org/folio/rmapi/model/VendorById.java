package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorById extends Vendor {

  @JsonProperty("proxy")
  private Proxy proxy;

  @JsonIgnore
  private String vendorToken;

  @JsonProperty("vendorToken")
  private TokenInfo vendorByIdToken;

  public Proxy getProxy() {
    return proxy;
  }

  public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }

  public TokenInfo getVendorByIdToken() {
    return vendorByIdToken;
  }

  public void setVendorByIdToken(TokenInfo vendorByIdToken) {
    this.vendorByIdToken = vendorByIdToken;
  }
}
