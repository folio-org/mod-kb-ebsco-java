package org.folio.rest.converter.costperuse;

import static org.apache.commons.lang3.math.NumberUtils.DOUBLE_ZERO;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.convertParameters;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getPackageTitlesTotalCost;
import static org.folio.rest.util.IdParser.getResourceId;
import static org.folio.rest.util.IdParser.resourceIdToString;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.client.uc.model.UCCostAnalysis;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.PublicationType;
import org.folio.rest.jaxrs.model.ResourceCostAnalysisAttributes;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollection;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollectionItem;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.ResourceCostPerUseCollectionResult;

@Component
public class ResourceCostPerUseCollectionConverter
  implements Converter<ResourceCostPerUseCollectionResult, ResourceCostPerUseCollection> {

  @Override
  public ResourceCostPerUseCollection convert(@NotNull ResourceCostPerUseCollectionResult source) {
    return new ResourceCostPerUseCollection()
      .withData(convertItems(source))
      .withParameters(convertParameters(source.getConfiguration()))
      .withMeta(new MetaTotalResults().withTotalResults(source.getHoldingInfos().size()))
      .withJsonapi(RestConstants.JSONAPI);
  }

  private List<ResourceCostPerUseCollectionItem> convertItems(ResourceCostPerUseCollectionResult source) {
    Double packageCost = Optional.ofNullable(source.getPackageCostPerUse().getAnalysis().getCurrent().getCost())
      .orElse(getPackageTitlesTotalCost(source.getTitlePackageCostMap()));
    return mapItems(source.getHoldingInfos(),
      dbHoldingInfo -> toResourceCostPerUseCollectionItem(dbHoldingInfo, source.getTitlePackageCostMap(), packageCost));
  }

  private ResourceCostPerUseCollectionItem toResourceCostPerUseCollectionItem(DbHoldingInfo dbHoldingInfo,
                                                                              Map<String, UCCostAnalysis> titlePackageCostMap,
                                                                              Double packageCost) {
    var ucCostAnalysis = titlePackageCostMap.get(getTitlePackageId(dbHoldingInfo));

    Double costPercent;
    if (DOUBLE_ZERO.equals(packageCost)) {
      costPercent = DOUBLE_ZERO;
    } else {
      costPercent = Optional.ofNullable(ucCostAnalysis.getCurrent().getCost())
        .map(titleCost -> titleCost / packageCost * 100)
        .orElse(DOUBLE_ZERO);
    }

    return new ResourceCostPerUseCollectionItem()
      .withResourceId(resourceIdToString(getResourceId(dbHoldingInfo)))
      .withType(ResourceCostPerUseCollectionItem.Type.RESOURCE_COST_PER_USE_ITEM)
      .withAttributes(new ResourceCostAnalysisAttributes()
        .withName(dbHoldingInfo.getPublicationTitle())
        .withPublicationType(getPublicationTitle(dbHoldingInfo))
        .withCost(ucCostAnalysis.getCurrent().getCost())
        .withUsage(ucCostAnalysis.getCurrent().getUsage())
        .withCostPerUse(ucCostAnalysis.getCurrent().getCostPerUse())
        .withPercent(costPercent)
      );
  }

  private PublicationType getPublicationTitle(DbHoldingInfo dbHoldingInfo) {
    String resourceType = dbHoldingInfo.getResourceType();
    PublicationType publicationType;
    if (resourceType.equals("Audio Book")) {
      publicationType = PublicationType.AUDIOBOOK;
    } else if (resourceType.equals("Web Site")){
      publicationType = PublicationType.WEBSITE;
    } else {
      publicationType = PublicationType.fromValue(dbHoldingInfo.getResourceType());
    }
    return publicationType;
  }

  private String getTitlePackageId(org.folio.repository.holdings.DbHoldingInfo dbHoldingInfo) {
    return dbHoldingInfo.getTitleId() + "." + dbHoldingInfo.getPackageId();
  }
}
