package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class CustomLabel {
  
  @JsonProperty("id")
  private Integer id;
  @JsonProperty("displayLabel")
  private String displayLabel;
  @JsonProperty("displayOnFullTextFinder")
  private Boolean displayOnFullTextFinder;
  @JsonProperty("displayOnPublicationFinder")
  private Boolean displayOnPublicationFinder;

}
