package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
public class CustomLabel {
  
  @JsonProperty("id")
  private Integer id;
  @JsonProperty("displayLabel")
  private String displayLabel;
  @JsonProperty("displayOnFullTextFinder")
  private Boolean displayOnFullTextFinder;
  @JsonProperty("displayOnPublicationFinder")
  private Boolean displayOnPublicationFinder;
  
  @JsonProperty("id")
  public Integer getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(Integer id) {
    this.id = id;
  }

  @JsonProperty("displayLabel")
  public String getDisplayLabel() {
    return displayLabel;
  }

  @JsonProperty("displayLabel")
  public void setDisplayLabel(String displayLabel) {
    this.displayLabel = displayLabel;
  }

  @JsonProperty("displayOnFullTextFinder")
  public Boolean getDisplayOnFullTextFinder() {
    return displayOnFullTextFinder;
  }

  @JsonProperty("displayOnFullTextFinder")
  public void setDisplayOnFullTextFinder(Boolean displayOnFullTextFinder) {
    this.displayOnFullTextFinder = displayOnFullTextFinder;
  }

  @JsonProperty("displayOnPublicationFinder")
  public Boolean getDisplayOnPublicationFinder() {
    return displayOnPublicationFinder;
  }

  @JsonProperty("displayOnPublicationFinder")
  public void setDisplayOnPublicationFinder(Boolean displayOnPublicationFinder) {
    this.displayOnPublicationFinder = displayOnPublicationFinder;
  }
}
