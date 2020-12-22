package org.folio.rest.converter.costperuse.export;

import static org.apache.commons.lang.math.NumberUtils.DOUBLE_ZERO;
import static org.apache.commons.lang.math.NumberUtils.INTEGER_ZERO;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.text.NumberFormat;

import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollectionItem;
import org.folio.service.uc.export.TitleExportModel;

@Component
public class PackageTitleCostPerUseConverter {

  public TitleExportModel convert(ResourceCostPerUseCollectionItem resourceCostPerUseCollectionItem, String platform, String year, String currency, NumberFormat currencyFormatter) {
    return TitleExportModel.builder()
      .title(resourceCostPerUseCollectionItem.getAttributes().getName())
      .type(resourceCostPerUseCollectionItem.getAttributes().getPublicationType().value())
      .usage(defaultIfNull(resourceCostPerUseCollectionItem.getAttributes().getUsage(), INTEGER_ZERO))
      .cost(roundCost(resourceCostPerUseCollectionItem.getAttributes().getCost(), currencyFormatter))
      .costPerUse(roundCost(resourceCostPerUseCollectionItem.getAttributes().getCostPerUse(), currencyFormatter))
      .percent(roundPercent(resourceCostPerUseCollectionItem.getAttributes().getPercent()))
      .platform(defaultIfNull(platform, PlatformType.ALL.value()))
      .currency(currency)
      .year(year)
      .build();
  }

  private String roundPercent(Double percent) {
    if (DOUBLE_ZERO.equals(percent) || percent == null) {
      return INTEGER_ZERO.toString();
    } else if (percent < 1){
      return "< 1 %";
    } else {
      return String.valueOf(Math.round(percent));
    }
  }

  private String roundCost(Double cost, NumberFormat currencyFormatter) {
    if (DOUBLE_ZERO.equals(cost) || cost == null) {
      cost = DOUBLE_ZERO;
    }
    return currencyFormatter.format(cost).replace("\u00a0", " ").trim();
  }
}
