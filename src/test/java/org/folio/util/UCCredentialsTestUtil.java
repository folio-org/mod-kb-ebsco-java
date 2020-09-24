package org.folio.util;

import static org.folio.repository.DbUtil.getUCCredentialsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.SqlQueryHelper.insertQuery;
import static org.folio.repository.uc.UCCredentialsTableConstants.CLIENT_ID_COLUMN;
import static org.folio.repository.uc.UCCredentialsTableConstants.CLIENT_SECRET_COLUMN;
import static org.folio.test.util.TestUtil.STUB_TENANT;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Tuple;

import org.folio.rest.persist.PostgresClient;

public class UCCredentialsTestUtil {

  public static final String STUB_CLIENT_ID = "client-id";
  public static final String STUB_CLIENT_SECRET = "client-secret";

  public static void setUpUCCredentials(Vertx vertx) {
    var future = new CompletableFuture<>();

    var query = prepareQuery(insertQuery(CLIENT_ID_COLUMN, CLIENT_SECRET_COLUMN), getUCCredentialsTableName(STUB_TENANT));
    Tuple params = Tuple.of(
      STUB_CLIENT_ID,
      STUB_CLIENT_SECRET
    );

    PostgresClient.getInstance(vertx).execute(query, params, event -> future.complete(null));
    future.join();
  }
}
