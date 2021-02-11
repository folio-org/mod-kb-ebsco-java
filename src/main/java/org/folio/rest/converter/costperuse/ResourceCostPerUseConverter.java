package org.folio.rest.converter.costperuse;

import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.convertParameters;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getAllPlatformUsages;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getCostAnalysisAttributes;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getTotalUsage;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.setNonPublisherUsage;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.setPublisherUsage;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.CostAnalysis;
import org.folio.rest.jaxrs.model.ResourceCostPerUse;
import org.folio.rest.jaxrs.model.ResourceCostPerUseDataAttributes;
import org.folio.rest.jaxrs.model.SpecificPlatformUsage;
import org.folio.rest.jaxrs.model.Usage;
import org.folio.rest.jaxrs.model.UsageTotals;
import org.folio.rest.util.IdParser;
import org.folio.rmapi.result.ResourceCostPerUseResult;

@Component
public class ResourceCostPerUseConverter implements Converter<ResourceCostPerUseResult, ResourceCostPerUse> {

  @Override
  public ResourceCostPerUse convert(@NotNull ResourceCostPerUseResult source) {
    ResourceCostPerUse resourceCostPerUse = new ResourceCostPerUse()
      .withResourceId(IdParser.resourceIdToString(source.getResourceId()))
      .withType(ResourceCostPerUse.Type.RESOURCE_COST_PER_USE);

    var ucTitleCostPerUse = source.getUcTitleCostPerUse();
    if (ucTitleCostPerUse.getUsage() == null || ucTitleCostPerUse.getUsage().getPlatforms() == null) {
      return resourceCostPerUse;
    }
    List<SpecificPlatformUsage> specificPlatformUsages = getAllPlatformUsages(ucTitleCostPerUse.getUsage());

    var usage = new Usage().withTotals(new UsageTotals());
    var analysis = new CostAnalysis();
    switch (source.getPlatformType()) {
      case PUBLISHER:
        setPublisherUsage(specificPlatformUsages, usage);

        analysis.setPublisherPlatforms(getCostAnalysisAttributes(ucTitleCostPerUse, usage.getTotals().getPublisher()));
        break;
      case NON_PUBLISHER:
        setNonPublisherUsage(specificPlatformUsages, usage);

        analysis.setNonPublisherPlatforms(getCostAnalysisAttributes(ucTitleCostPerUse, usage.getTotals().getNonPublisher()));
        break;
      default:
        setPublisherUsage(specificPlatformUsages, usage);
        setNonPublisherUsage(specificPlatformUsages, usage);
        usage.setPlatforms(specificPlatformUsages);
        usage.getTotals().setAll(getTotalUsage(specificPlatformUsages));

        analysis.setPublisherPlatforms(getCostAnalysisAttributes(ucTitleCostPerUse, usage.getTotals().getPublisher()));
        analysis.setNonPublisherPlatforms(getCostAnalysisAttributes(ucTitleCostPerUse, usage.getTotals().getNonPublisher()));
        analysis.setAllPlatforms(getCostAnalysisAttributes(ucTitleCostPerUse, usage.getTotals().getAll()));
        break;
    }

    return resourceCostPerUse
      .withAttributes(new ResourceCostPerUseDataAttributes()
        .withUsage(usage)
        .withAnalysis(analysis)
        .withParameters(convertParameters(source.getConfiguration()))
      );
  }
}
