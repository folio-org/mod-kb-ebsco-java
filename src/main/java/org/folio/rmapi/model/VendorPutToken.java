package org.folio.rmapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VendorPutToken {

   @JsonProperty("value")
  private Object value;

  @JsonProperty("value")
  public Object getValue() {
    return value;
  }

  @JsonProperty("value")
  public void setValue(Object value) {
    this.value = value;
  }
}
