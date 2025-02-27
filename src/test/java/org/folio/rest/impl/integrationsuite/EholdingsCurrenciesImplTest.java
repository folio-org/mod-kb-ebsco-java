package org.folio.rest.impl.integrationsuite;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.rest.impl.WireMockTestBase;
import org.folio.rest.jaxrs.model.Currency;
import org.folio.rest.jaxrs.model.CurrencyCollection;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class EholdingsCurrenciesImplTest extends WireMockTestBase {

  public static final String CURRENCIES_ENDPOINT = "/eholdings/currencies";

  @Test
  public void shouldReturnAccessTypeCollectionOnGet() {
    CurrencyCollection actual = getWithStatus(CURRENCIES_ENDPOINT, SC_OK).as(CurrencyCollection.class);
    getWithStatus(CURRENCIES_ENDPOINT, SC_OK).as(CurrencyCollection.class);

    assertThat(actual.getMeta().getTotalResults(), greaterThan(0));
    assertThat(actual.getData(), hasSize(actual.getMeta().getTotalResults()));
    assertThat(actual.getData().getFirst(), notNullValue());
    assertThat(actual.getData().getFirst(),
      allOf(
        hasProperty("id", equalTo("AFN")),
        hasProperty("type", equalTo(Currency.Type.CURRENCIES))
      )
    );
    assertThat(actual.getData().getFirst().getAttributes(),
      allOf(
        hasProperty("code", equalTo("AFN")),
        hasProperty("description", equalTo("Afghan Afghani"))
      )
    );
  }
}
