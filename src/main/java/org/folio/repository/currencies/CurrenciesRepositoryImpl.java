package org.folio.repository.currencies;

import static org.folio.common.LogUtils.logSelectQueryInfoLevel;
import static org.folio.repository.DbUtil.getCurrenciesTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.currencies.CurrenciesConstants.CODE_COLUMN;
import static org.folio.repository.currencies.CurrenciesConstants.DESCRIPTION_COLUMN;
import static org.folio.repository.currencies.CurrenciesConstants.SELECT_CURRENCIES;
import static org.folio.util.FutureUtils.mapResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import org.folio.db.RowSetUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.repository.DbUtil;

@Repository
public class CurrenciesRepositoryImpl implements CurrenciesRepository {

  private static final Logger LOG = LogManager.getLogger(CurrenciesRepositoryImpl.class);

  private final Vertx vertx;
  private final DBExceptionTranslator excTranslator;

  public CurrenciesRepositoryImpl(Vertx vertx, DBExceptionTranslator excTranslator) {
    this.vertx = vertx;
    this.excTranslator = excTranslator;
  }

  @Override
  public CompletableFuture<List<DbCurrency>> findAll(String tenant) {
    String query = prepareQuery(SELECT_CURRENCIES, getCurrenciesTableName(tenant));

    logSelectQueryInfoLevel(LOG, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    DbUtil.pgClient(tenant, vertx).execute(query, promise);

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
