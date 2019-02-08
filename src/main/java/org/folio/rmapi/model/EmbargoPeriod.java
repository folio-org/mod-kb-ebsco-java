package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbargoPeriod {

  @JsonProperty("embargoUnit")
  private String embargoUnit;

  @JsonProperty("embargoValue")
  private int embargoValue;

}
