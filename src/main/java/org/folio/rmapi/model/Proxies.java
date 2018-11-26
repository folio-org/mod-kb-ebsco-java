package org.folio.rmapi.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Proxies {

  @JsonProperty("proxies")
  private List<ProxyWithUrl> proxyList;


  public List<ProxyWithUrl> getProxyList() {
    return proxyList != null ? proxyList : Collections.emptyList();
  }

  public void setProxyList(List<ProxyWithUrl> proxyList) {
    this.proxyList = proxyList;
  }
}
