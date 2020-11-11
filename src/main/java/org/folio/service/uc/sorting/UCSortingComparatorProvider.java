package org.folio.service.uc.sorting;

import java.util.Comparator;

import org.folio.rest.jaxrs.model.Order;

public interface UCSortingComparatorProvider<I> {

  Comparator<I> get(CostPerUseSort sorting, Order order);

}
