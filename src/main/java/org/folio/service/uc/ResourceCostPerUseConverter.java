package org.folio.service.uc;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import org.folio.client.uc.model.UCPlatformUsage;
import org.folio.client.uc.model.UCTitleCost;
import org.folio.client.uc.model.UCTitleCostUsage;
import org.folio.rest.jaxrs.model.CostAnalysis;
import org.folio.rest.jaxrs.model.CostAnalysisAttributes;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.PlatformUsage;
import org.folio.rest.jaxrs.model.ResourceCostPerUse;
import org.folio.rest.jaxrs.model.ResourceCostPerUseDataAttributes;
import org.folio.rest.jaxrs.model.SpecificPlatformUsage;
import org.folio.rest.jaxrs.model.Usage;
import org.folio.rest.jaxrs.model.UsageTotals;

@Component
public class ResourceCostPerUseConverter {

  public ResourceCostPerUse convert(UCTitleCost ucTitleCost, PlatformType platformType) {
    UCTitleCostUsage ucTitleCostUsage = ucTitleCost.getUsage();
    List<SpecificPlatformUsage> specificPlatformUsages = ucTitleCostUsage.getPlatforms().entrySet()
      .stream()
      .map(entry -> {
        String platformName = entry.getKey();
        UCPlatformUsage ucPlatformUsage = entry.getValue();
        List<Integer> ucPlatformUsageCounts = ucPlatformUsage.getCounts();
        Boolean isPublisherPlatform = ucPlatformUsage.getPublisherPlatform();
        return new SpecificPlatformUsage()
          .withName(platformName)
          .withCounts(ucPlatformUsageCounts)
          .withIsPublisherPlatform(isPublisherPlatform)
          .withTotal(sum(ucPlatformUsageCounts));
      })
      .collect(Collectors.toList());

    Usage usage = new Usage().withTotals(new UsageTotals());
    CostAnalysis analysis = new CostAnalysis();
    if (platformType == PlatformType.PUBLISHER) {
      setPublisherPlatform(specificPlatformUsages, usage);

      CostAnalysisAttributes attributes = getCostAnalysisAttributes(ucTitleCost, usage.getTotals().getPublisher());
      analysis.setPublisherPlatforms(attributes);
    } else if (platformType == PlatformType.NON_PUBLISHER) {
      setNonPublisherPlatform(specificPlatformUsages, usage);

      CostAnalysisAttributes attributes = getCostAnalysisAttributes(ucTitleCost, usage.getTotals().getNonPublisher());
      analysis.setNonPublisherPlatforms(attributes);
    } else {
      setPublisherPlatform(specificPlatformUsages, usage);
      setNonPublisherPlatform(specificPlatformUsages, usage);
      usage.setPlatforms(specificPlatformUsages);
      usage.getTotals().setAll(getTotalUsage(specificPlatformUsages));

      analysis.setPublisherPlatforms(getCostAnalysisAttributes(ucTitleCost, usage.getTotals().getPublisher()));
      analysis.setNonPublisherPlatforms(getCostAnalysisAttributes(ucTitleCost, usage.getTotals().getNonPublisher()));
      analysis.setAllPlatforms(getCostAnalysisAttributes(ucTitleCost, usage.getTotals().getAll()));
    }

    return new ResourceCostPerUse().withType(ResourceCostPerUse.Type.RESOURCE_COST_PER_USE)
      .withAttributes(new ResourceCostPerUseDataAttributes()
        .withUsage(usage)
        .withAnalysis(analysis)
      );
  }

  private CostAnalysisAttributes getCostAnalysisAttributes(UCTitleCost ucTitleCost, PlatformUsage publisher) {
    CostAnalysisAttributes analysisAttributes = new CostAnalysisAttributes();
    analysisAttributes.setCost(ucTitleCost.getAnalysis().getCurrent().getCost());
    analysisAttributes.setUsage(publisher.getTotal());
    analysisAttributes.setCostPerUse(analysisAttributes.getCost() / analysisAttributes.getUsage());
    return analysisAttributes;
  }

  private void setNonPublisherPlatform(List<SpecificPlatformUsage> specificPlatformUsages, Usage usage) {
    List<SpecificPlatformUsage> nonPublisherPlatformUsages = specificPlatformUsages.stream()
      .filter(Predicate.not(SpecificPlatformUsage::getIsPublisherPlatform))
      .collect(Collectors.toList());
    usage.setPlatforms(nonPublisherPlatformUsages);
    usage.getTotals().setNonPublisher(getTotalUsage(nonPublisherPlatformUsages));
  }

  private void setPublisherPlatform(List<SpecificPlatformUsage> specificPlatformUsages, Usage usage) {
    List<SpecificPlatformUsage> publisherPlatformUsages = specificPlatformUsages.stream()
      .filter(SpecificPlatformUsage::getIsPublisherPlatform)
      .collect(Collectors.toList());
    usage.setPlatforms(publisherPlatformUsages);
    usage.getTotals().setPublisher(getTotalUsage(publisherPlatformUsages));
  }

  private PlatformUsage getTotalUsage(List<SpecificPlatformUsage> platformUsages) {
    PlatformUsage totalUsage = new PlatformUsage();
    List<Integer> totalCounts = IntStream.range(0, 12)
      .map(monthIndex -> platformUsages.stream()
        .map(SpecificPlatformUsage::getCounts)
        .map(integers -> integers.get(monthIndex))
        .filter(Objects::nonNull)
        .mapToInt(Integer::intValue)
        .sum())
      .boxed()
      .collect(Collectors.toList());
    totalUsage.setCounts(totalCounts);
    totalUsage.setTotal(sum(totalCounts));
    return totalUsage;
  }

  private int sum(List<Integer> integers) {
    return integers.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
  }
}
