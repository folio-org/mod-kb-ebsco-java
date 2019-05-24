package org.folio.rest.repository.holdings;

import java.util.Arrays;

public enum LoadStatus {

  COMPLETED("Completed"), IN_PROGRESS("In progress"), FAILED("Failed");

  private String value;

  LoadStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static LoadStatus fromValue(String value) {
    return Arrays.stream(values())
      .filter(statusValue -> statusValue.value.equalsIgnoreCase(value))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("LoadStatus with value " + value + " doesn't exist"));
  }
}
