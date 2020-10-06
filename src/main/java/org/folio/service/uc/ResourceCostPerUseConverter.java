package org.folio.service.uc;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.IterableUtils;
import org.springframework.stereotype.Component;

import org.folio.client.uc.model.UCPlatformUsage;
import org.folio.client.uc.model.UCTitleCostPerUse;
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

  public ResourceCostPerUse convert(UCTitleCostPerUse ucTitleCostPerUse, PlatformType platformType) {
    ResourceCostPerUse resourceCostPerUse = new ResourceCostPerUse().withType(ResourceCostPerUse.Type.RESOURCE_COST_PER_USE);

    if (ucTitleCostPerUse.getUsage() == null || ucTitleCostPerUse.getUsage().getPlatforms() == null) {
      return resourceCostPerUse;
    }
    var ucTitleCostUsage = ucTitleCostPerUse.getUsage();
    List<SpecificPlatformUsage> specificPlatformUsages = ucTitleCostUsage.getPlatforms().entrySet()
      .stream()
      .map(this::toSpecificPlatformUsage)
      .collect(Collectors.toList());

    var usage = new Usage().withTotals(new UsageTotals());
    var analysis = new CostAnalysis();
    if (platformType == PlatformType.PUBLISHER) {
      setPublisherPlatform(specificPlatformUsages, usage);

      var attributes = getCostAnalysisAttributes(ucTitleCostPerUse, usage.getTotals().getPublisher());
      analysis.setPublisherPlatforms(attributes);
    } else if (platformType == PlatformType.NON_PUBLISHER) {
      setNonPublisherPlatform(specificPlatformUsages, usage);

      var attributes = getCostAnalysisAttributes(ucTitleCostPerUse, usage.getTotals().getNonPublisher());
      analysis.setNonPublisherPlatforms(attributes);
    } else {
      setPublisherPlatform(specificPlatformUsages, usage);
      setNonPublisherPlatform(specificPlatformUsages, usage);
      usage.setPlatforms(specificPlatformUsages);
      usage.getTotals().setAll(getTotalUsage(specificPlatformUsages));

      analysis.setPublisherPlatforms(getCostAnalysisAttributes(ucTitleCostPerUse, usage.getTotals().getPublisher()));
      analysis.setNonPublisherPlatforms(getCostAnalysisAttributes(ucTitleCostPerUse, usage.getTotals().getNonPublisher()));
      analysis.setAllPlatforms(getCostAnalysisAttributes(ucTitleCostPerUse, usage.getTotals().getAll()));
    }

    return resourceCostPerUse
      .withAttributes(new ResourceCostPerUseDataAttributes()
        .withUsage(usage)
        .withAnalysis(analysis)
      );
  }

  private SpecificPlatformUsage toSpecificPlatformUsage(Map.Entry<String, UCPlatformUsage> entry) {
    UCPlatformUsage ucPlatformUsage = entry.getValue();
    List<Integer> ucPlatformUsageCounts = ucPlatformUsage.getCounts();
    Boolean isPublisherPlatform = ucPlatformUsage.getPublisherPlatform();
    return new SpecificPlatformUsage()
      .withName(entry.getKey())
      .withCounts(ucPlatformUsageCounts)
      .withIsPublisherPlatform(isPublisherPlatform)
      .withTotal(sum(ucPlatformUsageCounts));
  }

  private CostAnalysisAttributes getCostAnalysisAttributes(UCTitleCostPerUse ucTitleCostPerUse, PlatformUsage publisher) {
    var analysisAttributes = new CostAnalysisAttributes();
    if (ucTitleCostPerUse.getAnalysis() != null
      && ucTitleCostPerUse.getAnalysis().getCurrent() != null
      && ucTitleCostPerUse.getAnalysis().getCurrent().getCost() != null) {
      analysisAttributes.setCost(ucTitleCostPerUse.getAnalysis().getCurrent().getCost());
      analysisAttributes.setUsage(publisher.getTotal());
      analysisAttributes.setCostPerUse(analysisAttributes.getCost() / analysisAttributes.getUsage());
    }
    return analysisAttributes;
  }

  private void setNonPublisherPlatform(List<SpecificPlatformUsage> specificPlatformUsages, Usage usage) {
    var nonPublisherPlatformUsages = specificPlatformUsages.stream()
      .filter(Predicate.not(SpecificPlatformUsage::getIsPublisherPlatform))
      .collect(Collectors.toList());
    usage.setPlatforms(nonPublisherPlatformUsages);
    usage.getTotals().setNonPublisher(getTotalUsage(nonPublisherPlatformUsages));
  }

  private void setPublisherPlatform(List<SpecificPlatformUsage> specificPlatformUsages, Usage usage) {
    var publisherPlatformUsages = specificPlatformUsages.stream()
      .filter(SpecificPlatformUsage::getIsPublisherPlatform)
      .collect(Collectors.toList());
    usage.setPlatforms(publisherPlatformUsages);
    usage.getTotals().setPublisher(getTotalUsage(publisherPlatformUsages));
  }

  private PlatformUsage getTotalUsage(List<SpecificPlatformUsage> platformUsages) {
    var totalUsage = new PlatformUsage();
    var totalCounts = IntStream.range(0, 12)
      .boxed()
      .map(monthIndex -> getMonthSum(platformUsages, monthIndex))
      .collect(Collectors.toList());

    if (IterableUtils.matchesAll(totalCounts, Objects::isNull)) {
      return null;
    }
    totalUsage.setCounts(totalCounts);
    totalUsage.setTotal(sum(totalCounts));
    return totalUsage;
  }

  private Integer getMonthSum(List<SpecificPlatformUsage> platformUsages, int monthIndex) {
    List<Integer> countsByMonth = platformUsages.stream()
      .map(SpecificPlatformUsage::getCounts)
      .map(integers -> integers.get(monthIndex))
      .collect(Collectors.toList());
    if (IterableUtils.matchesAll(countsByMonth, Objects::isNull)) {
      return null;
    }
    return countsByMonth.stream()
      .filter(Objects::nonNull)
      .mapToInt(Integer::intValue)
      .sum();
  }

  private int sum(List<Integer> integers) {
    return integers.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
  }
}
