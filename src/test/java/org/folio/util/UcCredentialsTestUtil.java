package org.folio.util;

import static org.folio.db.RowSetUtils.isEmpty;
import static org.folio.db.RowSetUtils.mapFirstItem;
import static org.folio.repository.DbUtil.getUcCredentialsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.SqlQueryHelper.limitQuery;
import static org.folio.repository.SqlQueryHelper.selectQuery;
import static org.folio.repository.uc.UcCredentialsTableConstants.CLIENT_ID_COLUMN;
import static org.folio.repository.uc.UcCredentialsTableConstants.CLIENT_SECRET_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.concurrent.CompletableFuture;
import org.folio.repository.uc.DbUcCredentials;
import org.folio.rest.persist.PostgresClient;

public class UcCredentialsTestUtil {

  public static final String UC_CREDENTIALS_ENDPOINT = "eholdings/uc-credentials";

  public static final String STUB_CLIENT_ID = "client-id";
  public static final String STUB_CLIENT_SECRET = "client-secret";

  public static void setUpUcCredentials(Vertx vertx) {
    var future = new CompletableFuture<>();

    var query =
      prepareQuery(insertQuery(CLIENT_ID_COLUMN, CLIENT_SECRET_COLUMN), getUcCredentialsTableName(STUB_TENANT));
    Tuple params = Tuple.of(
      STUB_CLIENT_ID,
      STUB_CLIENT_SECRET
    );

    PostgresClient.getInstance(vertx).execute(query, params, event -> future.complete(null));
    future.join();
  }

  public static DbUcCredentials getUcCredentials(Vertx vertx) {
    var future = new CompletableFuture<DbUcCredentials>();

    var query = prepareQuery(selectQuery() + " " + limitQuery(1), getUcCredentialsTableName(STUB_TENANT));
    PostgresClient.getInstance(vertx).select(query, event -> future.complete(mapCredentials(event.result())));

    return future.join();
  }

  private static DbUcCredentials mapCredentials(RowSet<Row> rows) {
    if (isEmpty(rows)) {
      return null;
    } else {
      return mapFirstItem(rows, row -> {
        var clientId = row.getString(CLIENT_ID_COLUMN);
        var clientSecret = row.getString(CLIENT_SECRET_COLUMN);
        return new DbUcCredentials(clientId, clientSecret);
      });
    }
  }
}
