package org.folio.rest.converter.costperuse;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.IterableUtils;

import org.folio.client.uc.configuration.CommonUCConfiguration;
import org.folio.client.uc.model.UCCostAnalysis;
import org.folio.client.uc.model.UCCostAnalysisDetails;
import org.folio.client.uc.model.UCPlatformUsage;
import org.folio.client.uc.model.UCTitleCostPerUse;
import org.folio.client.uc.model.UCUsage;
import org.folio.rest.jaxrs.model.CostAnalysisAttributes;
import org.folio.rest.jaxrs.model.CostPerUseParameters;
import org.folio.rest.jaxrs.model.Month;
import org.folio.rest.jaxrs.model.PlatformUsage;
import org.folio.rest.jaxrs.model.SpecificPlatformUsage;
import org.folio.rest.jaxrs.model.Usage;

final class CostPerUseConverterUtils {

  private CostPerUseConverterUtils() {
  }

  static List<SpecificPlatformUsage> getAllPlatformUsages(UCUsage uCUsage) {
    return uCUsage.getPlatforms().entrySet()
      .stream()
      .map(CostPerUseConverterUtils::toSpecificPlatformUsage)
      .collect(Collectors.toList());
  }

  static List<SpecificPlatformUsage> getNonPublisherUsages(List<SpecificPlatformUsage> specificPlatformUsages) {
    return specificPlatformUsages.stream()
      .filter(Predicate.not(SpecificPlatformUsage::getIsPublisherPlatform))
      .collect(Collectors.toList());
  }

  static List<SpecificPlatformUsage> getPublisherUsages(List<SpecificPlatformUsage> specificPlatformUsages) {
    return specificPlatformUsages.stream()
      .filter(SpecificPlatformUsage::getIsPublisherPlatform)
      .collect(Collectors.toList());
  }

  static SpecificPlatformUsage toSpecificPlatformUsage(Map.Entry<String, UCPlatformUsage> entry) {
    UCPlatformUsage ucPlatformUsage = entry.getValue();
    List<Integer> ucPlatformUsageCounts = ucPlatformUsage.getCounts();
    Boolean isPublisherPlatform = ucPlatformUsage.getPublisherPlatform();
    return new SpecificPlatformUsage()
      .withName(entry.getKey())
      .withCounts(ucPlatformUsageCounts)
      .withIsPublisherPlatform(isPublisherPlatform)
      .withTotal(sum(ucPlatformUsageCounts));
  }

  static CostAnalysisAttributes getCostAnalysisAttributes(UCTitleCostPerUse ucTitleCostPerUse, PlatformUsage publisher) {
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

  static void setNonPublisherUsage(List<SpecificPlatformUsage> specificPlatformUsages, Usage usage) {
    var nonPublisherPlatformUsages = getNonPublisherUsages(specificPlatformUsages);
    usage.setPlatforms(nonPublisherPlatformUsages);
    usage.getTotals().setNonPublisher(getTotalUsage(nonPublisherPlatformUsages));
  }

  static void setPublisherUsage(List<SpecificPlatformUsage> specificPlatformUsages, Usage usage) {
    var publisherPlatformUsages = getPublisherUsages(specificPlatformUsages);
    usage.setPlatforms(publisherPlatformUsages);
    usage.getTotals().setPublisher(getTotalUsage(publisherPlatformUsages));
  }

  static PlatformUsage getTotalUsage(List<SpecificPlatformUsage> platformUsages) {
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

  static Integer getMonthSum(List<SpecificPlatformUsage> platformUsages, int monthIndex) {
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

  static int sum(List<Integer> integers) {
    return integers.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
  }

  static CostPerUseParameters convertParameters(CommonUCConfiguration configuration) {
    return new CostPerUseParameters()
      .withStartMonth(Month.fromValue(configuration.getFiscalMonth()))
      .withCurrency(configuration.getAnalysisCurrency());
  }

  public static double getPackageTitlesTotalCost(Map<String, UCCostAnalysis> titlePackageCost) {
    return titlePackageCost.values().stream()
      .map(UCCostAnalysis::getCurrent)
      .filter(Objects::nonNull)
      .map(UCCostAnalysisDetails::getCost)
      .filter(Objects::nonNull)
      .mapToDouble(Double::doubleValue)
      .sum();
  }
}
