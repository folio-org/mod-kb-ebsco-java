package org.folio.rest.converter.costperuse.export;

import static org.folio.common.ListUtils.mapItems;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.folio.rest.jaxrs.model.ResourceCostPerUseCollection;
import org.folio.service.locale.LocaleSettings;
import org.folio.service.uc.export.TitleExportModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PackageTitlesCostPerUseCollectionToExportConverter {

  @Autowired
  private PackageTitleCostPerUseConverter resourceCostPerUseExportItemConverter;

  public List<TitleExportModel> convert(ResourceCostPerUseCollection resourceCostPerUseCollection, String platform,
                                        String year, LocaleSettings localeSettings) {
    var data = resourceCostPerUseCollection.getData();
    var currency = resourceCostPerUseCollection.getParameters().getCurrency();
    NumberFormat numberFormat = getNumberFormat(localeSettings);
    return mapItems(data,
      item -> resourceCostPerUseExportItemConverter.convert(item, platform, year, currency, numberFormat));
  }

  private NumberFormat getNumberFormat(LocaleSettings localeSettings) {
    Locale userLocale = Locale.forLanguageTag(localeSettings.getLocale());
    NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(userLocale);
    currencyFormatter.setRoundingMode(RoundingMode.HALF_UP);
    DecimalFormatSymbols decimalFormatSymbols = ((DecimalFormat) currencyFormatter).getDecimalFormatSymbols();
    decimalFormatSymbols.setCurrencySymbol("");
    ((DecimalFormat) currencyFormatter).setDecimalFormatSymbols(decimalFormatSymbols);
    return currencyFormatter;
  }
}
