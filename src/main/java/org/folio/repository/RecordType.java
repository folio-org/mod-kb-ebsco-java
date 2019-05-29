package org.folio.repository;

public enum RecordType {

  PROVIDER("provider"), PACKAGE("package"), TITLE("title"), RESOURCE("resource");

  private String value;

  RecordType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
  
  public static RecordType fromValue(String value) {
    return valueOf(value.toUpperCase());
  }
}
