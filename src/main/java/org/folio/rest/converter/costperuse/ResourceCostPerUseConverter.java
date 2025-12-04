package org.folio.rest.converter.costperuse;

import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.convertParameters;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getAllPlatformUsages;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getCostAnalysisAttributes;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getTotalUsage;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.setNonPublisherUsage;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.setPublisherUsage;

import java.util.List;
import org.folio.client.uc.model.UcTitleCostPerUse;
import org.folio.rest.jaxrs.model.CostAnalysis;
import org.folio.rest.jaxrs.model.ResourceCostPerUse;
import org.folio.rest.jaxrs.model.ResourceCostPerUseDataAttributes;
import org.folio.rest.jaxrs.model.SpecificPlatformUsage;
import org.folio.rest.jaxrs.model.Usage;
import org.folio.rest.jaxrs.model.UsageTotals;
import org.folio.rest.util.IdParser;
import org.folio.rmapi.result.ResourceCostPerUseResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ResourceCostPerUseConverter implements Converter<ResourceCostPerUseResult, ResourceCostPerUse> {

  @Override
  public ResourceCostPerUse convert(ResourceCostPerUseResult source) {
    ResourceCostPerUse resourceCostPerUse = new ResourceCostPerUse()
      .withResourceId(IdParser.resourceIdToString(source.getResourceId()))
      .withType(ResourceCostPerUse.Type.RESOURCE_COST_PER_USE);

    var ucTitleCostPerUse = source.getUcTitleCostPerUse();
    if (ucTitleCostPerUse.usage() == null || ucTitleCostPerUse.usage().platforms() == null) {
      return resourceCostPerUse;
    }
    List<SpecificPlatformUsage> specificPlatformUsages = getAllPlatformUsages(ucTitleCostPerUse.usage());

    var usage = new Usage().withTotals(new UsageTotals());
    var analysis = new CostAnalysis();
    processPlatformUsages(source, specificPlatformUsages, usage, analysis, ucTitleCostPerUse);

    return resourceCostPerUse
      .withAttributes(new ResourceCostPerUseDataAttributes()
        .withUsage(usage)
        .withAnalysis(analysis)
        .withParameters(convertParameters(source.getConfiguration()))
      );
  }

  private void processPlatformUsages(ResourceCostPerUseResult source,
                                     List<SpecificPlatformUsage> specificPlatformUsages,
                                     Usage usage, CostAnalysis analysis, UcTitleCostPerUse titleCostPerUse) {
    switch (source.getPlatformType()) {
      case PUBLISHER -> {
        setPublisherUsage(specificPlatformUsages, usage);
        analysis.setPublisherPlatforms(getCostAnalysisAttributes(titleCostPerUse, usage.getTotals().getPublisher()));
      }
      case NON_PUBLISHER -> {
        setNonPublisherUsage(specificPlatformUsages, usage);
        analysis.setNonPublisherPlatforms(
          getCostAnalysisAttributes(titleCostPerUse, usage.getTotals().getNonPublisher()));
      }
      default -> {
        setPublisherUsage(specificPlatformUsages, usage);
        setNonPublisherUsage(specificPlatformUsages, usage);
        usage.setPlatforms(specificPlatformUsages);
        usage.getTotals().setAll(getTotalUsage(specificPlatformUsages));
        analysis.setPublisherPlatforms(getCostAnalysisAttributes(titleCostPerUse, usage.getTotals().getPublisher()));
        analysis.setNonPublisherPlatforms(
          getCostAnalysisAttributes(titleCostPerUse, usage.getTotals().getNonPublisher()));
        analysis.setAllPlatforms(getCostAnalysisAttributes(titleCostPerUse, usage.getTotals().getAll()));
      }
    }
  }
}
