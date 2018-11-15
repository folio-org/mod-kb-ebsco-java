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
  
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getDisplayLabel() {
    return displayLabel;
  }

  public void setDisplayLabel(String displayLabel) {
    this.displayLabel = displayLabel;
  }

  public Boolean getDisplayOnFullTextFinder() {
    return displayOnFullTextFinder;
  }

  public void setDisplayOnFullTextFinder(Boolean displayOnFullTextFinder) {
    this.displayOnFullTextFinder = displayOnFullTextFinder;
  }

  public Boolean getDisplayOnPublicationFinder() {
    return displayOnPublicationFinder;
  }

  public void setDisplayOnPublicationFinder(Boolean displayOnPublicationFinder) {
    this.displayOnPublicationFinder = displayOnPublicationFinder;
  }
}
