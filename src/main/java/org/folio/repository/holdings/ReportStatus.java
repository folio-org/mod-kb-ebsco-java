package org.folio.repository.holdings;

import java.util.Arrays;

public enum ReportStatus {
  COMPLETED("COMPLETED"), IN_PROGRESS("IN_PROGRESS"), FAILED("FAILED");

  private String value;

  ReportStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static ReportStatus fromValue(String value) {
    return Arrays.stream(values())
      .filter(statusValue -> statusValue.value.equalsIgnoreCase(value))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("DeltaReportStatus with value " + value + " doesn't exist"));
  }
}
