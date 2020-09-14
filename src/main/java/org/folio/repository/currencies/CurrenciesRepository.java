package org.folio.repository.currencies;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CurrenciesRepository {

  CompletableFuture<List<DbCurrency>> findAll(String tenant);
}
