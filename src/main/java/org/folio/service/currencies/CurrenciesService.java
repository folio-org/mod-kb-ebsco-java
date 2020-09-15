package org.folio.service.currencies;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.CurrencyCollection;

public interface CurrenciesService {

  CompletableFuture<CurrencyCollection> fetchCurrencyCollection(Map<String, String> okapiHeaders);
}
