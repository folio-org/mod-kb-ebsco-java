package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Error {
  @JsonProperty("code")
  private String code;
  @JsonProperty("message")
  private String message;
  @JsonProperty("subCode")
  private String subCode;

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public String getSubCode() {
    return subCode;
  }
}
