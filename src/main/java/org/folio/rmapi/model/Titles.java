package org.folio.rmapi.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Titles {

  @JsonProperty("titles")
  private List<Title> titleList;

  @JsonProperty("totalResults")
  private Integer totalResults;
  
}
