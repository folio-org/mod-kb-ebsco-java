package org.folio.rest.converter.export;

import static org.junit.Assert.assertEquals;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import org.apache.commons.lang3.RandomStringUtils;
import org.folio.rest.converter.costperuse.export.PackageTitleCostPerUseConverter;
import org.folio.rest.jaxrs.model.PlatformType;
import org.folio.rest.jaxrs.model.PublicationType;
import org.folio.rest.jaxrs.model.ResourceCostAnalysisAttributes;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollectionItem;
import org.folio.service.uc.export.TitleExportModel;
import org.junit.Before;
import org.junit.Test;

public class PackageTitleCostPerUseConverterTest {

  private PackageTitleCostPerUseConverter converter;
  private NumberFormat currencyFormatter;

  @Before
  public void setUp() {
    converter = new PackageTitleCostPerUseConverter();
  }

  @Test
  public void shouldRoundPercentToLessThanOne() {
    ResourceCostPerUseCollectionItem item = new ResourceCostPerUseCollectionItem()
      .withAttributes(
        new ResourceCostAnalysisAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(10))
          .withPublicationType(PublicationType.PROCEEDINGS)
          .withCost(10.0)
          .withCostPerUse(0.2)
          .withUsage(50)
          .withPercent(0.9)
      );
    setNumberFormat(Locale.US);
    TitleExportModel itemConverted =
      converter.convert(item, PlatformType.NON_PUBLISHER.value(), "2020", "USD", currencyFormatter);
    assertEquals("< 1 %", itemConverted.getPercent());
  }

  @Test
  public void shouldRoundPercentToLower() {
    ResourceCostPerUseCollectionItem item = new ResourceCostPerUseCollectionItem()
      .withAttributes(
        new ResourceCostAnalysisAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(10))
          .withPublicationType(PublicationType.PROCEEDINGS)
          .withCost(10.0)
          .withCostPerUse(0.2)
          .withUsage(50)
          .withPercent(3.3)
      );
    setNumberFormat(Locale.US);
    TitleExportModel itemConverted =
      converter.convert(item, PlatformType.NON_PUBLISHER.value(), "2020", "USD", currencyFormatter);
    assertEquals("3 %", itemConverted.getPercent());
  }

  @Test
  public void shouldRoundPercentToHigher() {
    ResourceCostPerUseCollectionItem item = new ResourceCostPerUseCollectionItem()
      .withAttributes(
        new ResourceCostAnalysisAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(10))
          .withPublicationType(PublicationType.PROCEEDINGS)
          .withCost(10.0)
          .withCostPerUse(0.2)
          .withUsage(50)
          .withPercent(3.5)
      );
    setNumberFormat(Locale.US);
    TitleExportModel itemConverted =
      converter.convert(item, PlatformType.NON_PUBLISHER.value(), "2020", "USD", currencyFormatter);
    assertEquals("4 %", itemConverted.getPercent());
  }

  @Test
  public void shouldRoundDownCosts() {
    ResourceCostPerUseCollectionItem item = new ResourceCostPerUseCollectionItem()
      .withAttributes(
        new ResourceCostAnalysisAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(10))
          .withPublicationType(PublicationType.PROCEEDINGS)
          .withCost(10.4212)
          .withCostPerUse(0.33333333333333)
          .withUsage(3)
          .withPercent(3.3)
      );
    setNumberFormat(Locale.US);
    TitleExportModel itemConverted =
      converter.convert(item, PlatformType.NON_PUBLISHER.value(), "2020", "USD", currencyFormatter);
    assertEquals("10.42", itemConverted.getCost());
    assertEquals("0.33", itemConverted.getCostPerUse());
  }

  @Test
  public void shouldRoundUpCosts() {
    ResourceCostPerUseCollectionItem item = new ResourceCostPerUseCollectionItem()
      .withAttributes(
        new ResourceCostAnalysisAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(10))
          .withPublicationType(PublicationType.PROCEEDINGS)
          .withCost(10.5678)
          .withCostPerUse(1.42857143333333)
          .withUsage(7)
          .withPercent(3.3)
      );
    setNumberFormat(Locale.US);
    TitleExportModel itemConverted =
      converter.convert(item, PlatformType.NON_PUBLISHER.value(), "2020", "USD", currencyFormatter);
    assertEquals("10.57", itemConverted.getCost());
    assertEquals("1.43", itemConverted.getCostPerUse());
  }

  @Test
  public void shouldFormatCostsToUsFormat() {
    ResourceCostPerUseCollectionItem item = new ResourceCostPerUseCollectionItem()
      .withAttributes(
        new ResourceCostAnalysisAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(10))
          .withPublicationType(PublicationType.PROCEEDINGS)
          .withCost(10000.0)
          .withCostPerUse(3333.33333333333333)
          .withUsage(3)
          .withPercent(3.3)
      );
    setNumberFormat(Locale.US);
    TitleExportModel itemConverted =
      converter.convert(item, PlatformType.NON_PUBLISHER.value(), "2020", "USD", currencyFormatter);
    assertEquals("10,000.00", itemConverted.getCost());
    assertEquals("3,333.33", itemConverted.getCostPerUse());
  }

  @Test
  public void shouldFormatCostsToGbFormat() {
    ResourceCostPerUseCollectionItem item = new ResourceCostPerUseCollectionItem()
      .withAttributes(
        new ResourceCostAnalysisAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(10))
          .withPublicationType(PublicationType.PROCEEDINGS)
          .withCost(10000.0)
          .withCostPerUse(3333.33333333333333)
          .withUsage(3)
          .withPercent(3.3)
      );
    setNumberFormat(Locale.GERMANY);
    TitleExportModel itemConverted =
      converter.convert(item, PlatformType.NON_PUBLISHER.value(), "2020", "USD", currencyFormatter);
    assertEquals("10.000,00", itemConverted.getCost());
    assertEquals("3.333,33", itemConverted.getCostPerUse());
  }

  @Test
  public void shouldFormatCostsToFrFormat() {
    ResourceCostPerUseCollectionItem item = new ResourceCostPerUseCollectionItem()
      .withAttributes(
        new ResourceCostAnalysisAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(10))
          .withPublicationType(PublicationType.PROCEEDINGS)
          .withCost(10000.0)
          .withCostPerUse(3333.33333333333333)
          .withUsage(3)
          .withPercent(3.3)
      );
    setNumberFormat(Locale.FRANCE);
    TitleExportModel itemConverted =
      converter.convert(item, PlatformType.NON_PUBLISHER.value(), "2020", "USD", currencyFormatter);
    assertEquals("10 000,00", itemConverted.getCost());
    assertEquals("3 333,33", itemConverted.getCostPerUse());
  }

  @Test
  public void shouldFormatCostsToUkFormat() {
    ResourceCostPerUseCollectionItem item = new ResourceCostPerUseCollectionItem()
      .withAttributes(
        new ResourceCostAnalysisAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(10))
          .withPublicationType(PublicationType.PROCEEDINGS)
          .withCost(10000.0)
          .withCostPerUse(3333.33733333333333)
          .withUsage(3)
          .withPercent(3.3)
      );
    setNumberFormat(Locale.UK);
    TitleExportModel itemConverted =
      converter.convert(item, PlatformType.NON_PUBLISHER.value(), "2020", "USD", currencyFormatter);
    assertEquals("10,000.00", itemConverted.getCost());
    assertEquals("3,333.34", itemConverted.getCostPerUse());
  }

  @Test
  public void shouldFormatCostsToKoreanFormat() {
    ResourceCostPerUseCollectionItem item = new ResourceCostPerUseCollectionItem()
      .withAttributes(
        new ResourceCostAnalysisAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(10))
          .withPublicationType(PublicationType.PROCEEDINGS)
          .withCost(10000.0)
          .withCostPerUse(3333.33733333333333)
          .withUsage(3)
          .withPercent(3.3)
      );
    setNumberFormat(Locale.KOREA);
    TitleExportModel itemConverted =
      converter.convert(item, PlatformType.NON_PUBLISHER.value(), "2020", "USD", currencyFormatter);
    assertEquals("10,000", itemConverted.getCost());
    assertEquals("3,333", itemConverted.getCostPerUse());
  }

  @Test
  public void shouldFormatCostsToJordanFormat() {
    ResourceCostPerUseCollectionItem item = new ResourceCostPerUseCollectionItem()
      .withAttributes(
        new ResourceCostAnalysisAttributes()
          .withName(RandomStringUtils.randomAlphanumeric(10))
          .withPublicationType(PublicationType.PROCEEDINGS)
          .withCost(10000.0)
          .withCostPerUse(3333.33733333333333)
          .withUsage(3)
          .withPercent(3.3)
      );
    Locale jordanLocale = new Locale("ar", "JO");
    setNumberFormat(jordanLocale);
    TitleExportModel itemConverted =
      converter.convert(item, PlatformType.NON_PUBLISHER.value(), "2020", "USD", currencyFormatter);
    assertEquals("١٠٬٠٠٠٫٠٠٠", itemConverted.getCost());
    assertEquals("٣٬٣٣٣٫٣٣٧", itemConverted.getCostPerUse());
  }

  private void setNumberFormat(Locale userLocale) {
    currencyFormatter = NumberFormat.getCurrencyInstance(userLocale);
    currencyFormatter.setRoundingMode(RoundingMode.HALF_UP);
    DecimalFormatSymbols decimalFormatSymbols = ((DecimalFormat) currencyFormatter).getDecimalFormatSymbols();
    decimalFormatSymbols.setCurrencySymbol("");
    ((DecimalFormat) currencyFormatter).setDecimalFormatSymbols(decimalFormatSymbols);
  }
}
