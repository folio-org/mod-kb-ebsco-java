package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VisibilityInfo {
  @JsonProperty("isHidden")
  private Boolean isHidden;

  @JsonProperty("reason")
  private String reason;

  public Boolean getHidden() {
    return isHidden;
  }

  public String getReason() {
    return reason;
  }

  public void setHidden(Boolean hidden) {
    isHidden = hidden;
  }
}
