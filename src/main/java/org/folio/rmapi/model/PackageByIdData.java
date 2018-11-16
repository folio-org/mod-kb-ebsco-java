package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PackageByIdData extends PackageData {
  @JsonProperty("proxy")
  private Proxy proxy;
  @JsonProperty("packageToken")
  private TokenInfo packageToken;

  public Proxy getProxy() {
    return proxy;
  }

  public TokenInfo getPackageToken() {
    return packageToken;
  }
}
