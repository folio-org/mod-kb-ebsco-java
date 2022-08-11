package org.folio.service.uc.sorting;

import static org.folio.service.uc.sorting.CostPerUseSort.COST;
import static org.folio.service.uc.sorting.CostPerUseSort.COSTPERUSE;
import static org.folio.service.uc.sorting.CostPerUseSort.PERCENT;
import static org.folio.service.uc.sorting.CostPerUseSort.TYPE;
import static org.folio.service.uc.sorting.CostPerUseSort.USAGE;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import org.folio.common.ComparatorUtils;
import org.folio.rest.jaxrs.model.Order;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollectionItem;

@UtilityClass
public class UcSortingComparatorProviders {

  public static UcSortingComparatorProvider<ResourceCostPerUseCollectionItem> forResources() {
    Function<ResourceCostPerUseCollectionItem, String> nameExtractor = o -> o.getAttributes().getName();

    Map<CostPerUseSort, Comparator<ResourceCostPerUseCollectionItem>> comparatorPerSort =
      new EnumMap<>(CostPerUseSort.class);

    comparatorPerSort.put(TYPE,
      Comparator.<ResourceCostPerUseCollectionItem, String>comparing(
          o -> o.getAttributes().getPublicationType().value())
        .thenComparing(nameExtractor));
    comparatorPerSort.put(COST,
      ComparatorUtils.<ResourceCostPerUseCollectionItem>nullFirstDouble(o -> o.getAttributes().getCost())
        .thenComparing(nameExtractor));
    comparatorPerSort.put(USAGE,
      ComparatorUtils.<ResourceCostPerUseCollectionItem>nullFirstInteger(o -> o.getAttributes().getUsage())
        .thenComparing(nameExtractor));
    comparatorPerSort.put(COSTPERUSE,
      ComparatorUtils.<ResourceCostPerUseCollectionItem>nullFirstDouble(o -> o.getAttributes().getCostPerUse())
        .thenComparing(nameExtractor));
    comparatorPerSort.put(PERCENT,
      ComparatorUtils.<ResourceCostPerUseCollectionItem>nullFirstDouble(o -> o.getAttributes().getPercent())
        .thenComparing(nameExtractor));

    return (sorting, order) -> {
      Comparator<ResourceCostPerUseCollectionItem> comparator =
        comparatorPerSort.getOrDefault(sorting, Comparator.comparing(nameExtractor));

      return order == Order.ASC ? comparator : comparator.reversed();
    };
  }
}
