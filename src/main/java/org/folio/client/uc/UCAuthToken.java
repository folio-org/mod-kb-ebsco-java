package org.folio.client.uc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class UCAuthToken {

  @JsonProperty("access_token")
  String accessToken;
  @JsonProperty("token_type")
  String tokenType;
  @JsonProperty("expires_in")
  Long expiresIn;
  @JsonProperty("scope")
  String scope;
}
