package org.folio.repository.uc;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.LogUtils.logDeleteQueryInfoLevel;
import static org.folio.common.LogUtils.logInsertQueryInfoLevel;
import static org.folio.common.LogUtils.logSelectQueryInfoLevel;
import static org.folio.db.RowSetUtils.isEmpty;
import static org.folio.db.RowSetUtils.mapFirstItem;
import static org.folio.repository.DbUtil.getUCCredentialsTableName;
import static org.folio.repository.DbUtil.pgClient;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.uc.UCCredentialsTableConstants.CLIENT_ID_COLUMN;
import static org.folio.repository.uc.UCCredentialsTableConstants.CLIENT_SECRET_COLUMN;
import static org.folio.repository.uc.UCCredentialsTableConstants.DELETE_UC_CREDENTIALS;
import static org.folio.repository.uc.UCCredentialsTableConstants.SAVE_UC_CREDENTIALS;
import static org.folio.repository.uc.UCCredentialsTableConstants.SELECT_UC_CREDENTIALS;
import static org.folio.util.FutureUtils.mapResult;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import org.folio.db.exc.translation.DBExceptionTranslator;

@Component
public class UCCredentialsRepositoryImpl implements UCCredentialsRepository {

  private static final Logger LOG = LogManager.getLogger(UCCredentialsRepositoryImpl.class);

  private final Vertx vertx;
  private final DBExceptionTranslator excTranslator;

  public UCCredentialsRepositoryImpl(Vertx vertx, DBExceptionTranslator excTranslator) {
    this.vertx = vertx;
    this.excTranslator = excTranslator;
  }

  @Override
  public CompletableFuture<Optional<DbUCCredentials>> find(String tenant) {
    String query = prepareQuery(SELECT_UC_CREDENTIALS, getUCCredentialsTableName(tenant));

    logSelectQueryInfoLevel(LOG, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant, vertx).execute(query, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapUCCredentials);
  }

  @Override
  public CompletableFuture<Void> save(DbUCCredentials credentials, String tenant) {
    String query = prepareQuery(SAVE_UC_CREDENTIALS, getUCCredentialsTableName(tenant));

    var params = Tuple.of(credentials.getClientId(), credentials.getClientSecret());
    logInsertQueryInfoLevel(LOG, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant, vertx).execute(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Void> delete(String tenant) {
    String query = prepareQuery(DELETE_UC_CREDENTIALS, getUCCredentialsTableName(tenant));

    logDeleteQueryInfoLevel(LOG, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant, vertx).execute(query, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  private Optional<DbUCCredentials> mapUCCredentials(RowSet<Row> rows) {
    return isEmpty(rows)
      ? Optional.empty()
      : mapFirstItem(rows, row -> {
      String clientId = row.getString(CLIENT_ID_COLUMN);
      String clientSecret = row.getString(CLIENT_SECRET_COLUMN);
      return Optional.of(new DbUCCredentials(clientId, clientSecret));
    });
  }
}
