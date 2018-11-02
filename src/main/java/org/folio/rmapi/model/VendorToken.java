package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VendorToken {

  @JsonProperty("factName")
  private String factName;
  @JsonProperty("prompt")
  private String prompt;
  @JsonProperty("helpText")
  private String helpText;
  @JsonProperty("value")
  private Object value;

  @JsonProperty("factName")
  public String getFactName() {
    return factName;
  }

  @JsonProperty("factName")
  public void setFactName(String factName) {
    this.factName = factName;
  }

  @JsonProperty("prompt")
  public String getPrompt() {
    return prompt;
  }

  @JsonProperty("prompt")
  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  @JsonProperty("helpText")
  public String getHelpText() {
    return helpText;
  }

  @JsonProperty("helpText")
  public void setHelpText(String helpText) {
    this.helpText = helpText;
  }

  @JsonProperty("value")
  public Object getValue() {
    return value;
  }

  @JsonProperty("value")
  public void setValue(Object value) {
    this.value = value;
  }
}
