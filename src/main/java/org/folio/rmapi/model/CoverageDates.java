package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CoverageDates {
  @JsonProperty("beginCoverage")
  private String beginCoverage;

  @JsonProperty("endCoverage")
  private String endCoverage;

  public String getBeginCoverage() {
    return beginCoverage;
  }

  public String getEndCoverage() {
    return endCoverage;
  }
}
