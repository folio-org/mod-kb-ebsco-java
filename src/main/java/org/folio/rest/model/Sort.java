package org.folio.rest.model;

public enum Sort {
  RELEVANCE("relevance"), NAME("name");

  private String value;

  Sort(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static boolean contains(String value) {
    for (Sort c : Sort.values()) {
      if (c.name().equals(value)) {
        return true;
      }
    }

    return false;
  }
}
