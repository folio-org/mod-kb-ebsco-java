package org.folio.rest.impl;

import static org.folio.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.folio.rest.jaxrs.model.Currency;
import org.folio.rest.jaxrs.model.CurrencyCollection;
import org.folio.util.IntegrationTestBase;
import org.junit.jupiter.api.Test;

class EholdingsCurrenciesImplIntegrationTest extends IntegrationTestBase {

  private static final String CURRENCIES_ENDPOINT = "/eholdings/currencies";

  @Test
  void shouldReturnAccessTypeCollectionOnGet() {
    var actual = getWithStatus(CURRENCIES_ENDPOINT, SC_OK).as(CurrencyCollection.class);

    assertTrue(actual.getMeta().getTotalResults() > 0);
    assertEquals(actual.getData().size(), actual.getMeta().getTotalResults());
    assertNotNull(actual.getData().getFirst());
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
