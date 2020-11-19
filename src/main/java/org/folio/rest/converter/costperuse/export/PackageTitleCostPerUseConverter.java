package org.folio.rest.converter.costperuse.export;

import static org.apache.commons.lang.math.NumberUtils.DOUBLE_ZERO;
import static org.apache.commons.lang.math.NumberUtils.INTEGER_ZERO;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.ResourceCostPerUseCollectionItem;
import org.folio.service.uc.export.TitleExportModel;

@Component
public class PackageTitleCostPerUseConverter implements Converter<ResourceCostPerUseCollectionItem, TitleExportModel> {

  @Override
  public TitleExportModel convert(ResourceCostPerUseCollectionItem resourceCostPerUseCollectionItem) {
    return TitleExportModel.builder()
      .title(resourceCostPerUseCollectionItem.getAttributes().getName())
      .type(resourceCostPerUseCollectionItem.getAttributes().getPublicationType().value())
      .cost(defaultIfNull(resourceCostPerUseCollectionItem.getAttributes().getCost(), DOUBLE_ZERO))
      .usage(defaultIfNull(resourceCostPerUseCollectionItem.getAttributes().getUsage(), INTEGER_ZERO))
      .percent(defaultIfNull(resourceCostPerUseCollectionItem.getAttributes().getPercent(), DOUBLE_ZERO))
      .costPerUse(defaultIfNull(resourceCostPerUseCollectionItem.getAttributes().getCostPerUse(), DOUBLE_ZERO))
      .year("1029")
      .build();
  }
}
