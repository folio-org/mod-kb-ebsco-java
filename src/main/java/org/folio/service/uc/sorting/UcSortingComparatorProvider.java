package org.folio.service.uc.sorting;

import java.util.Comparator;
import org.folio.rest.jaxrs.model.Order;

public interface UcSortingComparatorProvider<I> {

  Comparator<I> get(CostPerUseSort sorting, Order order);

}
