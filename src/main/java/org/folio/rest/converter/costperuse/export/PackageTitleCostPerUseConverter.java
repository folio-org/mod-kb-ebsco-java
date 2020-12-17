package org.folio.rest.converter.costperuse.export;

import static org.apache.commons.lang.math.NumberUtils.DOUBLE_ZERO;
import static org.apache.commons.lang.math.NumberUtils.INTEGER_ZERO;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollectionItem;
import org.folio.service.uc.export.TitleExportModel;

@Component
public class PackageTitleCostPerUseConverter {

  public TitleExportModel convert(ResourceCostPerUseCollectionItem resourceCostPerUseCollectionItem, String platform, String year, String currency) {
    return TitleExportModel.builder()
      .title(resourceCostPerUseCollectionItem.getAttributes().getName())
      .type(resourceCostPerUseCollectionItem.getAttributes().getPublicationType().value())
      .cost(defaultIfNull(resourceCostPerUseCollectionItem.getAttributes().getCost(), DOUBLE_ZERO))
      .usage(defaultIfNull(resourceCostPerUseCollectionItem.getAttributes().getUsage(), INTEGER_ZERO))
      .percent(defaultIfNull(resourceCostPerUseCollectionItem.getAttributes().getPercent(), DOUBLE_ZERO))
      .costPerUse(defaultIfNull(resourceCostPerUseCollectionItem.getAttributes().getCostPerUse(), DOUBLE_ZERO))
      .platform(defaultIfNull(platform, PlatformType.ALL.value()))
      .currency(currency)
      .year(year)
      .build();
  }
}
