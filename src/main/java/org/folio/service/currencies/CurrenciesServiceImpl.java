package org.folio.service.currencies;

import static org.folio.rest.util.RequestHeadersUtil.tenantId;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.cache.VertxCache;
import org.folio.repository.currencies.CurrenciesRepository;
import org.folio.repository.currencies.DbCurrency;
import org.folio.rest.jaxrs.model.CurrencyCollection;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

@Service
public class CurrenciesServiceImpl implements CurrenciesService {

  private static final String CACHE_KEY = "currencies";

  private final CurrenciesRepository repository;
  private final Converter<List<DbCurrency>, CurrencyCollection> converter;
  private final VertxCache<String, CurrencyCollection> currenciesCache;

  public CurrenciesServiceImpl(CurrenciesRepository repository,
                               Converter<List<DbCurrency>, CurrencyCollection> converter,
                               VertxCache<String, CurrencyCollection> currenciesCache) {
    this.repository = repository;
    this.converter = converter;
    this.currenciesCache = currenciesCache;
  }

  @Override
  public CompletableFuture<CurrencyCollection> fetchCurrencyCollection(Map<String, String> okapiHeaders) {
    return currenciesCache.getValueOrLoad(CACHE_KEY, () -> loadCurrencyCollection(okapiHeaders));
  }

  private CompletableFuture<CurrencyCollection> loadCurrencyCollection(Map<String, String> okapiHeaders) {
    return repository.findAll(tenantId(okapiHeaders)).thenApply(converter::convert);
  }
}
