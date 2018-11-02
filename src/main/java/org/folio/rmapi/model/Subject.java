package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Subject {

  @JsonProperty("type")
  private String type;

  @JsonProperty("subject")
  private String subjectValue;

  public String getType() {
    return type;
  }

  public String getSubject() {
    return subjectValue;
  }
}
