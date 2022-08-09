package org.folio.rest.converter.costperuse;

import static org.apache.commons.lang3.math.NumberUtils.DOUBLE_ZERO;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.convertParameters;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getAllPlatformUsages;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getNonPublisherUsages;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getPackageTitlesTotalCost;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getPublisherUsages;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getTotalUsage;

import java.util.List;
import org.folio.rest.jaxrs.model.CostAnalysis;
import org.folio.rest.jaxrs.model.CostAnalysisAttributes;
import org.folio.rest.jaxrs.model.PackageCostPerUse;
import org.folio.rest.jaxrs.model.PackageCostPerUseDataAttributes;
import org.folio.rest.jaxrs.model.SpecificPlatformUsage;
import org.folio.rmapi.result.PackageCostPerUseResult;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PackageCostPerUseConverter implements Converter<PackageCostPerUseResult, PackageCostPerUse> {

  @Override
  public PackageCostPerUse convert(@NotNull PackageCostPerUseResult source) {
    var ucPackageCostPerUse = source.getUcPackageCostPerUse();
    var titlePackageCost = source.getTitlePackageCostMap();

    Double cost;
    if (titlePackageCost != null) {
      cost = getPackageTitlesTotalCost(titlePackageCost);
    } else {
      cost = ucPackageCostPerUse.getAnalysis().getCurrent().getCost();
    }

    var costAnalysis = new CostAnalysis();
    var allPlatformUsages = getAllPlatformUsages(ucPackageCostPerUse.getUsage());
    switch (source.getPlatformType()) {
      case PUBLISHER:
        costAnalysis.setPublisherPlatforms(getCostAnalysisAttributes(getPublisherUsages(allPlatformUsages), cost));
        break;
      case NON_PUBLISHER:
        costAnalysis.setNonPublisherPlatforms(
          getCostAnalysisAttributes(getNonPublisherUsages(allPlatformUsages), cost));
        break;
      default:
        costAnalysis.setPublisherPlatforms(getCostAnalysisAttributes(getPublisherUsages(allPlatformUsages), cost));
        costAnalysis.setNonPublisherPlatforms(
          getCostAnalysisAttributes(getNonPublisherUsages(allPlatformUsages), cost));
        costAnalysis.setAllPlatforms(getCostAnalysisAttributes(allPlatformUsages, cost));
    }
    return new PackageCostPerUse()
      .withPackageId(source.getPackageId())
      .withType(PackageCostPerUse.Type.PACKAGE_COST_PER_USE)
      .withAttributes(new PackageCostPerUseDataAttributes()
        .withAnalysis(costAnalysis)
        .withParameters(convertParameters(source.getConfiguration()))
      );
  }

  private CostAnalysisAttributes getCostAnalysisAttributes(List<SpecificPlatformUsage> usages, Double cost) {
    var platformUsage = getTotalUsage(usages);
    var usageCount = platformUsage == null ? INTEGER_ZERO : platformUsage.getTotal();
    double costPerUse;
    if (INTEGER_ZERO.equals(usageCount) && DOUBLE_ZERO.equals(cost)) {
      costPerUse = DOUBLE_ZERO;
    } else {
      costPerUse = cost / usageCount;
    }
    return new CostAnalysisAttributes()
      .withCost(cost)
      .withUsage(usageCount)
      .withCostPerUse(costPerUse);
  }
}
