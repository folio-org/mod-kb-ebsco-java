package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Contributor {

  @JsonProperty("type")
  private String type;

  @JsonProperty("contributor")
  private String titleContributor;

  public String getType() {
    return type;
  }

  public String getTitleContributor() {
    return titleContributor;
  }
}
