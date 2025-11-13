package org.folio.rest.converter.costperuse;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.convertParameters;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getAllPlatformUsages;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.getTotalUsage;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.setNonPublisherUsage;
import static org.folio.rest.converter.costperuse.CostPerUseConverterUtils.setPublisherUsage;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.math.NumberUtils;
import org.folio.client.uc.model.UcCostAnalysis;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.EmbargoPeriod;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.HoldingsCostAnalysisAttributes;
import org.folio.rest.jaxrs.model.PlatformUsage;
import org.folio.rest.jaxrs.model.SpecificPlatformUsage;
import org.folio.rest.jaxrs.model.TitleCostAnalysis;
import org.folio.rest.jaxrs.model.TitleCostPerUse;
import org.folio.rest.jaxrs.model.TitleCostPerUseDataAttributes;
import org.folio.rest.jaxrs.model.Usage;
import org.folio.rest.jaxrs.model.UsageTotals;
import org.folio.rmapi.result.TitleCostPerUseResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class TitleCostPerUseConverter implements Converter<TitleCostPerUseResult, TitleCostPerUse> {

  private final Converter<EmbargoPeriod, org.folio.rest.jaxrs.model.EmbargoPeriod> embargoPeriodConverter;
  private final Converter<List<CoverageDates>, List<Coverage>> coveragesConverter;

  public TitleCostPerUseConverter(
    Converter<EmbargoPeriod, org.folio.rest.jaxrs.model.EmbargoPeriod> embargoPeriodConverter,
    Converter<List<CoverageDates>, List<Coverage>> coveragesConverter) {
    this.embargoPeriodConverter = embargoPeriodConverter;
    this.coveragesConverter = coveragesConverter;
  }

  @Override
  public TitleCostPerUse convert(@NonNull TitleCostPerUseResult source) {
    TitleCostPerUse titleCostPerUse = new TitleCostPerUse()
      .withTitleId(source.getTitleId())
      .withType(TitleCostPerUse.Type.TITLE_COST_PER_USE);

    var ucTitleCostPerUse = source.getUcTitleCostPerUse();
    if (ucTitleCostPerUse.usage() == null || ucTitleCostPerUse.usage().platforms() == null) {
      return titleCostPerUse;
    }

    List<SpecificPlatformUsage> specificPlatformUsages = getAllPlatformUsages(ucTitleCostPerUse.usage());

    var usage = new Usage().withTotals(new UsageTotals());
    var analysis = new TitleCostAnalysis();
    Integer totalUsage = NumberUtils.INTEGER_ZERO;

    switch (source.getPlatformType()) {
      case PUBLISHER:
        setPublisherUsage(specificPlatformUsages, usage);
        PlatformUsage publisherPlatformUsage = usage.getTotals().getPublisher();
        if (publisherPlatformUsage != null) {
          totalUsage = publisherPlatformUsage.getTotal();
        }
        break;
      case NON_PUBLISHER:
        setNonPublisherUsage(specificPlatformUsages, usage);
        PlatformUsage nonPublisherPlatformUsage = usage.getTotals().getNonPublisher();
        if (nonPublisherPlatformUsage != null) {
          totalUsage = nonPublisherPlatformUsage.getTotal();
        }
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
      .toList();
  }

  private HoldingsCostAnalysisAttributes toHoldingsCostAnalysis(
    Integer totalUsage, Map<String, UcCostAnalysis> titlePackageCostMap,
    org.folio.holdingsiq.model.CustomerResources customerResource) {
    var titleId = customerResource.getTitleId();
    var packageId = customerResource.getPackageId();
    var vendorId = customerResource.getVendorId();
    var titlePackageId = titleId + "." + packageId;
    var ucCostAnalysis = titlePackageCostMap.get(titlePackageId).current();

    var embargoPeriod = defineEmbargoType(customerResource);

    var customCoverageList = emptyIfNull(customerResource.getCustomCoverageList());
    var managedCoverageList = emptyIfNull(customerResource.getManagedCoverageList());
    var coverageDates = customCoverageList.isEmpty() ? managedCoverageList : customCoverageList;
    var cost = getIfNull(ucCostAnalysis.cost(), NumberUtils.DOUBLE_ZERO);
    return new HoldingsCostAnalysisAttributes()
      .withPackageId(vendorId + "-" + packageId)
      .withResourceId(vendorId + "-" + packageId + "-" + titleId)
      .withPackageName(customerResource.getPackageName())
      .withCost(cost)
      .withUsage(totalUsage)
      .withCostPerUse(cost / totalUsage)
      .withCoverageStatement(customerResource.getCoverageStatement())
      .withEmbargoPeriod(embargoPeriodConverter.convert(embargoPeriod))
      .withCoverages(coveragesConverter.convert(coverageDates));
  }

  private EmbargoPeriod defineEmbargoType(org.folio.holdingsiq.model.CustomerResources customerResource) {
    var customEmbargoPeriod = customerResource.getCustomEmbargoPeriod();
    if (customEmbargoPeriod != null && customEmbargoPeriod.getEmbargoUnit() != null) {
      return customEmbargoPeriod;
    } else {
      return customerResource.getManagedEmbargoPeriod();
    }
  }
}
