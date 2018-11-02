package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbargoPeriod {

  @JsonProperty("embargoUnit")
  private String embargoUnit;

  @JsonProperty("embargoValue")
  private Integer embargoValue;

  public String getEmbargoUnit() {
    return embargoUnit;
  }

  public Integer getEmbargoValue() {
    return embargoValue;
  }
}
