package org.folio.rest.converter.costperuse;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.convertParameters;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getSpecificPlatformUsages;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getTotalUsage;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.setNonPublisherUsage;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.setPublisherUsage;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.EmbargoPeriod;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.HoldingsCostAnalysisAttributes;
import org.folio.rest.jaxrs.model.SpecificPlatformUsage;
import org.folio.rest.jaxrs.model.TitleCostAnalysis;
import org.folio.rest.jaxrs.model.TitleCostPerUse;
import org.folio.rest.jaxrs.model.TitleCostPerUseDataAttributes;
import org.folio.rest.jaxrs.model.Usage;
import org.folio.rest.jaxrs.model.UsageTotals;
import org.folio.rmapi.result.TitleCostPerUseResult;

@Component
public class TitleCostPerUseConverter implements Converter<TitleCostPerUseResult, TitleCostPerUse> {

  @Autowired
  private Converter<EmbargoPeriod, org.folio.rest.jaxrs.model.EmbargoPeriod> embargoPeriodConverter;
  @Autowired
  private Converter<List<CoverageDates>, List<Coverage>> coveragesConverter;

  @Override
  public TitleCostPerUse convert(@NotNull TitleCostPerUseResult source) {
    TitleCostPerUse titleCostPerUse = new TitleCostPerUse()
      .withTitleId(source.getTitleId())
      .withType(TitleCostPerUse.Type.TITLE_COST_PER_USE);

    var ucTitleCostPerUse = source.getUcTitleCostPerUse();
    if (ucTitleCostPerUse.getUsage() == null || ucTitleCostPerUse.getUsage().getPlatforms() == null) {
      return titleCostPerUse;
    }

    List<SpecificPlatformUsage> specificPlatformUsages = getSpecificPlatformUsages(ucTitleCostPerUse.getUsage());

    var usage = new Usage().withTotals(new UsageTotals());
    var analysis = new TitleCostAnalysis();
    Integer totalUsage;

    switch (source.getPlatformType()) {
      case PUBLISHER:
        setPublisherUsage(specificPlatformUsages, usage);
        totalUsage = usage.getTotals().getPublisher().getTotal();
        break;
      case NON_PUBLISHER:
        setNonPublisherUsage(specificPlatformUsages, usage);
        totalUsage = usage.getTotals().getNonPublisher().getTotal();
        break;
      default:
        setPublisherUsage(specificPlatformUsages, usage);
        setNonPublisherUsage(specificPlatformUsages, usage);
        usage.setPlatforms(specificPlatformUsages);
        var platformUsage = getTotalUsage(specificPlatformUsages);
        usage.getTotals().setAll(platformUsage);

        totalUsage = platformUsage == null ? 0 : platformUsage.getTotal();
        break;
    }

    analysis.setHoldingsSummary(getHoldingsSummary(source, totalUsage));
    return titleCostPerUse
      .withAttributes(new TitleCostPerUseDataAttributes()
        .withUsage(usage)
        .withAnalysis(analysis)
        .withParameters(convertParameters(source.getConfiguration()))
      );
  }

  private List<HoldingsCostAnalysisAttributes> getHoldingsSummary(TitleCostPerUseResult source, Integer totalUsage) {
    var customerResources = source.getCustomerResources();
    var titlePackageCostMap = source.getTitlePackageCostMap();
    return customerResources.stream()
      .map(customerResource -> toHoldingsCostAnalysis(totalUsage, titlePackageCostMap, customerResource))
      .collect(Collectors.toList());
  }

  private HoldingsCostAnalysisAttributes toHoldingsCostAnalysis(Integer totalUsage,
                                                                java.util.Map<String, org.folio.client.uc.model.UCCostAnalysis> titlePackageCostMap,
                                                                org.folio.holdingsiq.model.CustomerResources customerResource) {
    var titleId = customerResource.getTitleId();
    var packageId = customerResource.getPackageId();
    var vendorId = customerResource.getVendorId();
    var titlePackageId = titleId + "." + packageId;
    var ucCostAnalysis = titlePackageCostMap.get(titlePackageId).getCurrent();

    var embargoPeriod =
      defaultIfNull(customerResource.getCustomEmbargoPeriod(), customerResource.getManagedEmbargoPeriod());

    var customCoverageList = emptyIfNull(customerResource.getCustomCoverageList());
    var managedCoverageList = emptyIfNull(customerResource.getManagedCoverageList());
    var coverageDates = customCoverageList.isEmpty() ? managedCoverageList : customCoverageList;
    var cost = defaultIfNull(ucCostAnalysis.getCost(), NumberUtils.DOUBLE_ZERO);
    return new HoldingsCostAnalysisAttributes()
      .withPackageId(vendorId + "-" + packageId)
      .withPackageName(customerResource.getPackageName())
      .withCost(cost)
      .withUsage(totalUsage)
      .withCostPerUse(cost / totalUsage)
      .withCoverageStatement(customerResource.getCoverageStatement())
      .withEmbargoPeriod(embargoPeriodConverter.convert(embargoPeriod))
      .withCoverages(coveragesConverter.convert(coverageDates));
  }
}
