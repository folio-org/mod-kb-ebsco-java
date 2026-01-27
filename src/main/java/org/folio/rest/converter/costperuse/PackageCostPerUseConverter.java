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
import java.util.Map;
import org.folio.client.uc.model.UcCostAnalysis;
import org.folio.client.uc.model.UcPackageCostPerUse;
import org.folio.rest.jaxrs.model.CostAnalysis;
import org.folio.rest.jaxrs.model.CostAnalysisAttributes;
import org.folio.rest.jaxrs.model.PackageCostPerUse;
import org.folio.rest.jaxrs.model.PackageCostPerUseDataAttributes;
import org.folio.rest.jaxrs.model.SpecificPlatformUsage;
import org.folio.rmapi.result.PackageCostPerUseResult;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PackageCostPerUseConverter implements Converter<PackageCostPerUseResult, PackageCostPerUse> {

  @Override
  public PackageCostPerUse convert(PackageCostPerUseResult source) {
    var ucPackageCostPerUse = source.getUcPackageCostPerUse();
    var titlePackageCost = source.getTitlePackageCostMap();

    var cost = processCost(titlePackageCost, ucPackageCostPerUse);
    var costAnalysis = new CostAnalysis();
    var allPlatformUsages = getAllPlatformUsages(ucPackageCostPerUse.usage());
    processPlatformUsages(source, costAnalysis, allPlatformUsages, cost);
    return new PackageCostPerUse()
      .withPackageId(source.getPackageId())
      .withType(PackageCostPerUse.Type.PACKAGE_COST_PER_USE)
      .withAttributes(new PackageCostPerUseDataAttributes()
        .withAnalysis(costAnalysis)
        .withParameters(convertParameters(source.getConfiguration()))
      );
  }

  private Double processCost(@Nullable Map<String, UcCostAnalysis> titlePackageCost,
                             UcPackageCostPerUse ucPackageCostPerUse) {
    Double cost;
    if (titlePackageCost != null) {
      cost = getPackageTitlesTotalCost(titlePackageCost);
    } else {
      cost = ucPackageCostPerUse.analysis().current().cost();
    }
    return cost;
  }

  private void processPlatformUsages(PackageCostPerUseResult source, CostAnalysis costAnalysis,
                                     List<SpecificPlatformUsage> usagesList, Double cost) {
    switch (source.getPlatformType()) {
      case PUBLISHER ->
        costAnalysis.setPublisherPlatforms(getCostAnalysisAttributes(getPublisherUsages(usagesList), cost));
      case NON_PUBLISHER ->
        costAnalysis.setNonPublisherPlatforms(getCostAnalysisAttributes(getNonPublisherUsages(usagesList), cost));
      default -> {
        costAnalysis.setPublisherPlatforms(getCostAnalysisAttributes(getPublisherUsages(usagesList), cost));
        costAnalysis.setNonPublisherPlatforms(getCostAnalysisAttributes(getNonPublisherUsages(usagesList), cost));
        costAnalysis.setAllPlatforms(getCostAnalysisAttributes(usagesList, cost));
      }
    }
  }

  private CostAnalysisAttributes getCostAnalysisAttributes(List<SpecificPlatformUsage> usages, Double cost) {
    var totalUsage = getTotalUsage(usages);
    if (totalUsage != null) {
      var usageCount = totalUsage.getTotal();
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
    } else {
      return new CostAnalysisAttributes()
        .withCost(cost);
    }
  }
}
