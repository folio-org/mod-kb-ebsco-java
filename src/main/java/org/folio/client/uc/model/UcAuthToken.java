package org.folio.client.uc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class UcAuthToken {

  @JsonProperty("access_token")
  String accessToken;
  @JsonProperty("token_type")
  String tokenType;
  @JsonProperty("expires_in")
  Long expiresIn;
  @JsonProperty("scope")
  String scope;
}
