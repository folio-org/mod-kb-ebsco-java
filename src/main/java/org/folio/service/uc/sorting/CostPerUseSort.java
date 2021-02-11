package org.folio.service.uc.sorting;

public enum CostPerUseSort {

  NAME, TYPE, COST, USAGE, COSTPERUSE, PERCENT;

  public static CostPerUseSort from(String value) {
    return CostPerUseSort.valueOf(value.toUpperCase());
  }

  public static boolean contains(String value) {
    for (CostPerUseSort c : CostPerUseSort.values()) {
      if (c.name().equalsIgnoreCase(value)) {
        return true;
      }
    }
    return false;
  }
}