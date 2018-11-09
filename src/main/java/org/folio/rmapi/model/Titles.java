package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Titles {

  @JsonProperty("titles")
  private List<Title> titleList;

  @JsonProperty("totalResults")
  private Integer totalResults;

  public List<Title> getTitleList() {
    return titleList;
  }

  public Integer getTotalResults() {
    return totalResults;
  }
}
