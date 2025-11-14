package org.folio.repository.currencies;

import static org.folio.common.LogUtils.logSelectQuery;
import static org.folio.repository.currencies.CurrenciesConstants.CODE_COLUMN;
import static org.folio.repository.currencies.CurrenciesConstants.DESCRIPTION_COLUMN;
import static org.folio.repository.currencies.CurrenciesConstants.selectCurrencies;
import static org.folio.util.FutureUtils.mapResult;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.log4j.Log4j2;
import org.folio.db.RowSetUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.repository.DbUtil;
import org.springframework.stereotype.Repository;

@Log4j2
@Repository
public class CurrenciesRepositoryImpl implements CurrenciesRepository {
  private final Vertx vertx;
  private final DBExceptionTranslator excTranslator;

  public CurrenciesRepositoryImpl(Vertx vertx, DBExceptionTranslator excTranslator) {
    this.vertx = vertx;
    this.excTranslator = excTranslator;
  }

  @Override
  public CompletableFuture<List<DbCurrency>> findAll(String tenant) {
    String query = selectCurrencies(tenant);

    logSelectQuery(log, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    DbUtil.pgClient(tenant, vertx).execute(query, promise::handle);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapCurrenciesCollection);
  }

  private List<DbCurrency> mapCurrenciesCollection(RowSet<Row> rows) {
    return RowSetUtils.mapItems(rows, row -> {
      String code = row.getString(CODE_COLUMN);
      String description = row.getString(DESCRIPTION_COLUMN);
      return new DbCurrency(code, description);
    });
  }
}
