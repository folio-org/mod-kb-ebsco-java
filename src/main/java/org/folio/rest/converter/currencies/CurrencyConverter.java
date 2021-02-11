package org.folio.rest.converter.currencies;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.repository.currencies.DbCurrency;
import org.folio.rest.jaxrs.model.Currency;
import org.folio.rest.jaxrs.model.CurrencyCollection;
import org.folio.rest.jaxrs.model.CurrencyDataAttributes;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.util.RestConstants;

@Component
public class CurrencyConverter implements Converter<List<DbCurrency>, CurrencyCollection> {

  @Override
  public CurrencyCollection convert(@NotNull List<DbCurrency> source) {
    return new CurrencyCollection()
      .withData(mapItems(source, this::toCurrency))
      .withMeta(new MetaTotalResults().withTotalResults(source.size()))
      .withJsonapi(RestConstants.JSONAPI);
  }

  private Currency toCurrency(DbCurrency dbCurrency) {
    return new Currency()
      .withId(dbCurrency.getCode())
      .withType(Currency.Type.CURRENCIES)
      .withAttributes(new CurrencyDataAttributes()
        .withCode(dbCurrency.getCode())
        .withDescription(dbCurrency.getDescription())
      );
  }
}
