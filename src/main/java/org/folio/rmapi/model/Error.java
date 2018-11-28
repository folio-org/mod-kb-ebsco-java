package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Error {
  @JsonProperty("code")
  private String code;
  @JsonProperty("message")
  private String message;
  @JsonProperty("subCode")
  private String subCode;

}
