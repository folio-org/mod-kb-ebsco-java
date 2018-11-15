package org.folio.rmapi.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RootProxyCustomLabels {

  @JsonProperty("proxy")
  private Proxy proxy;
  
  @JsonProperty("labels")
  private List<CustomLabel> labelList;

  public Proxy getProxy() {
    return proxy;
  }

  public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }
  
  public List<CustomLabel> getLabelList() {
    return labelList;
  }
  
  public void setLabelList(List<CustomLabel> labelList) {
    this.labelList = labelList;
  }
}
