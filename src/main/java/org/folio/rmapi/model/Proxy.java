package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Proxy {

  @JsonProperty("id")
  private String id;
  @JsonProperty("inherited")
  private Boolean inherited;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Boolean getInherited() {
    return inherited;
  }

  public void setInherited(Boolean inherited) {
    this.inherited = inherited;
  }
}
