package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProxyWithUrl {

  @JsonProperty(value = "id", required = true)
  private String id;
  @JsonProperty("name")
  private String name;
  @JsonProperty("urlMask")
  private String urlMask;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrlMask() {
    return urlMask;
  }

  public void setUrlMask(String urlMask) {
    this.urlMask = urlMask;
  }
}
