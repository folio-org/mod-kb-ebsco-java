package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProxyWithUrl {

  @JsonProperty(value = "id", required = true)
  private String id;
  @JsonProperty("name")
  private String name;
  @JsonProperty("urlMask")
  private String urlMask;

}
