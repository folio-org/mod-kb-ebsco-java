package org.folio.repository.uc;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.LogUtils.logDeleteQuery;
import static org.folio.common.LogUtils.logInsertQuery;
import static org.folio.common.LogUtils.logSelectQuery;
import static org.folio.db.RowSetUtils.isEmpty;
import static org.folio.db.RowSetUtils.mapFirstItem;
import static org.folio.repository.DbUtil.pgClient;
import static org.folio.repository.uc.UcCredentialsTableConstants.CLIENT_ID_COLUMN;
import static org.folio.repository.uc.UcCredentialsTableConstants.CLIENT_SECRET_COLUMN;
import static org.folio.repository.uc.UcCredentialsTableConstants.deleteUcCredentials;
import static org.folio.repository.uc.UcCredentialsTableConstants.saveUcCredentials;
import static org.folio.repository.uc.UcCredentialsTableConstants.selectUcCredentials;
import static org.folio.util.FutureUtils.mapResult;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.extern.log4j.Log4j2;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class UcCredentialsRepositoryImpl implements UcCredentialsRepository {
  private final Vertx vertx;
  private final DBExceptionTranslator excTranslator;

  public UcCredentialsRepositoryImpl(Vertx vertx, DBExceptionTranslator excTranslator) {
    this.vertx = vertx;
    this.excTranslator = excTranslator;
  }

  @Override
  public CompletableFuture<Optional<DbUcCredentials>> find(String tenant) {
    String query = selectUcCredentials(tenant);

    logSelectQuery(log, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant, vertx).execute(query, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapUcCredentials);
  }

  @Override
  public CompletableFuture<Void> save(DbUcCredentials credentials, String tenant) {
    String query = saveUcCredentials(tenant);

    var params = Tuple.of(credentials.getClientId(), credentials.getClientSecret());
    logInsertQuery(log, query, params, true);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant, vertx).execute(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Void> delete(String tenant) {
    String query = deleteUcCredentials(tenant);

    logDeleteQuery(log, query);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenant, vertx).execute(query, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  private Optional<DbUcCredentials> mapUcCredentials(RowSet<Row> rows) {
    return isEmpty(rows)
           ? Optional.empty()
           : mapFirstItem(rows, row -> {
             String clientId = row.getString(CLIENT_ID_COLUMN);
             String clientSecret = row.getString(CLIENT_SECRET_COLUMN);
             return Optional.of(new DbUcCredentials(clientId, clientSecret));
           });
  }
}
