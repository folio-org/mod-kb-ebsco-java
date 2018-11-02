package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Errors {
  @JsonProperty("errors")
  private List<Error> errorList;

  public List<Error> getErrors() {
    return errorList;
  }
}
