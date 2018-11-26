package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class TokenInfo {

  @JsonProperty("factName")
  private String factName;
  @JsonProperty("prompt")
  private String prompt;
  @JsonProperty("helpText")
  private String helpText;
  @JsonProperty("value")
  private Object value;

}
