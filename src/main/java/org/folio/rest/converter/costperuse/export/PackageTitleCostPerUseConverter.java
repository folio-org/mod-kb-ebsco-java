package org.folio.rest.converter.costperuse.export;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.math.NumberUtils.DOUBLE_ZERO;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

import java.text.NumberFormat;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollectionItem;
import org.folio.service.uc.export.TitleExportModel;
import org.springframework.stereotype.Component;

@Component
public class PackageTitleCostPerUseConverter {

  public TitleExportModel convert(ResourceCostPerUseCollectionItem resourceCostPerUseCollectionItem, String platform,
                                  String year, String currency, NumberFormat currencyFormatter) {
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
    String result;
    if (DOUBLE_ZERO.equals(percent) || percent == null) {
      result = INTEGER_ZERO.toString();
    } else if (percent < 1) {
      result = "< 1";
    } else {
      result = String.valueOf(Math.round(percent));
    }
    return result + " %";
  }

  private String roundCost(Double cost, NumberFormat currencyFormatter) {
    if (cost == null) {
      cost = DOUBLE_ZERO;
    }
    return currencyFormatter
      .format(cost)
      .replace("\u00a0", " ")
      .replace("\u202f", " ") // For French localization. See JDK-8225245
      .trim();

  }
}
