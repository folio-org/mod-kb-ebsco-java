package org.folio.rest.converter.costperuse;

import static org.apache.commons.lang3.math.NumberUtils.DOUBLE_ZERO;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.convertParameters;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getAllPlatformUsages;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getNonPublisherUsages;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getPublisherUsages;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getTotalUsage;
import static org.folio.rest.util.IdParser.getResourceId;
import static org.folio.rest.util.IdParser.resourceIdToString;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.folio.client.uc.model.UcCostAnalysis;
import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.PlatformUsage;
import org.folio.rest.jaxrs.model.PublicationType;
import org.folio.rest.jaxrs.model.ResourceCostAnalysisAttributes;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollection;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollectionItem;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.ResourceCostPerUseCollectionResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

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
    Integer packageUsage = calcPackageUsage(source);
    return mapItems(source.getHoldingInfos(),
      dbHoldingInfo -> toResourceCostPerUseCollectionItem(dbHoldingInfo, source.getTitlePackageCostMap(),
        packageUsage));
  }

  private Integer calcPackageUsage(ResourceCostPerUseCollectionResult source) {
    PlatformUsage totalUsage;
    var platformType = source.getPlatformType();
    var allPlatformUsages = getAllPlatformUsages(source.getPackageCostPerUse().getUsage());
    if (PlatformType.NON_PUBLISHER == platformType) {
      totalUsage = getTotalUsage(getNonPublisherUsages(allPlatformUsages));
    } else if (PlatformType.PUBLISHER == platformType) {
      totalUsage = getTotalUsage(getPublisherUsages(allPlatformUsages));
    } else {
      totalUsage = getTotalUsage(allPlatformUsages);
    }
    return Optional.ofNullable(totalUsage).map(PlatformUsage::getTotal).orElse(INTEGER_ZERO);
  }

  private ResourceCostPerUseCollectionItem toResourceCostPerUseCollectionItem(
    DbHoldingInfo dbHoldingInfo, Map<String, UcCostAnalysis> titlePackageCostMap, Integer packageUsage) {
    var ucCostAnalysis = titlePackageCostMap.get(getTitlePackageId(dbHoldingInfo));

    Double usagePercent;
    if (INTEGER_ZERO.equals(packageUsage)) {
      usagePercent = DOUBLE_ZERO;
    } else {
      usagePercent = Optional.ofNullable(ucCostAnalysis.getCurrent().getUsage())
        .map(titleUsage -> (double) titleUsage / packageUsage * 100)
        .orElse(DOUBLE_ZERO);
    }

    return new ResourceCostPerUseCollectionItem()
      .withResourceId(resourceIdToString(getResourceId(dbHoldingInfo)))
      .withType(ResourceCostPerUseCollectionItem.Type.RESOURCE_COST_PER_USE_ITEM)
      .withAttributes(new ResourceCostAnalysisAttributes()
        .withName(dbHoldingInfo.getPublicationTitle())
        .withPublicationType(PublicationType.fromValue(dbHoldingInfo.getResourceType()))
        .withCost(ucCostAnalysis.getCurrent().getCost())
        .withUsage(ucCostAnalysis.getCurrent().getUsage())
        .withCostPerUse(ucCostAnalysis.getCurrent().getCostPerUse())
        .withPercent(usagePercent)
      );
  }

  private String getTitlePackageId(org.folio.repository.holdings.DbHoldingInfo dbHoldingInfo) {
    return dbHoldingInfo.getTitleId() + "." + dbHoldingInfo.getPackageId();
  }
}
