package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorById extends Vendor {

  @JsonProperty("proxy")
  private Proxy proxy;

  public Proxy getProxy() {
    return proxy;
  }

  public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }
}
