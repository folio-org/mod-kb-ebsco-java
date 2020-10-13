package org.folio.rest.converter.costperuse;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.IterableUtils;

import org.folio.client.uc.configuration.CommonUCConfiguration;
import org.folio.client.uc.model.UCPlatformUsage;
import org.folio.client.uc.model.UCTitleCostPerUse;
import org.folio.client.uc.model.UCUsage;
import org.folio.rest.jaxrs.model.CostAnalysisAttributes;
import org.folio.rest.jaxrs.model.CostPerUseParameters;
import org.folio.rest.jaxrs.model.Month;
import org.folio.rest.jaxrs.model.PlatformUsage;
import org.folio.rest.jaxrs.model.SpecificPlatformUsage;
import org.folio.rest.jaxrs.model.Usage;

abstract class CommonCostPerUseConverter {

  protected List<SpecificPlatformUsage> getSpecificPlatformUsages(UCUsage uCUsage) {
    return uCUsage.getPlatforms().entrySet()
      .stream()
      .map(this::toSpecificPlatformUsage)
      .collect(Collectors.toList());
  }

  protected SpecificPlatformUsage toSpecificPlatformUsage(Map.Entry<String, UCPlatformUsage> entry) {
    UCPlatformUsage ucPlatformUsage = entry.getValue();
    List<Integer> ucPlatformUsageCounts = ucPlatformUsage.getCounts();
    Boolean isPublisherPlatform = ucPlatformUsage.getPublisherPlatform();
    return new SpecificPlatformUsage()
      .withName(entry.getKey())
      .withCounts(ucPlatformUsageCounts)
      .withIsPublisherPlatform(isPublisherPlatform)
      .withTotal(sum(ucPlatformUsageCounts));
  }

  protected CostAnalysisAttributes getCostAnalysisAttributes(UCTitleCostPerUse ucTitleCostPerUse, PlatformUsage publisher) {
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

  protected void setNonPublisherUsage(List<SpecificPlatformUsage> specificPlatformUsages, Usage usage) {
    var nonPublisherPlatformUsages = specificPlatformUsages.stream()
      .filter(Predicate.not(SpecificPlatformUsage::getIsPublisherPlatform))
      .collect(Collectors.toList());
    usage.setPlatforms(nonPublisherPlatformUsages);
    usage.getTotals().setNonPublisher(getTotalUsage(nonPublisherPlatformUsages));
  }

  protected void setPublisherUsage(List<SpecificPlatformUsage> specificPlatformUsages, Usage usage) {
    var publisherPlatformUsages = specificPlatformUsages.stream()
      .filter(SpecificPlatformUsage::getIsPublisherPlatform)
      .collect(Collectors.toList());
    usage.setPlatforms(publisherPlatformUsages);
    usage.getTotals().setPublisher(getTotalUsage(publisherPlatformUsages));
  }

  protected PlatformUsage getTotalUsage(List<SpecificPlatformUsage> platformUsages) {
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

  protected Integer getMonthSum(List<SpecificPlatformUsage> platformUsages, int monthIndex) {
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

  protected int sum(List<Integer> integers) {
    return integers.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
  }

  protected CostPerUseParameters convertParameters(CommonUCConfiguration configuration) {
    return new CostPerUseParameters()
      .withStartMonth(Month.fromValue(configuration.getFiscalMonth()))
      .withCurrency(configuration.getAnalysisCurrency());
  }
}
