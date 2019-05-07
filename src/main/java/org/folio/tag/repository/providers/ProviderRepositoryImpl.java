package org.folio.tag.repository.providers;

import static org.folio.tag.repository.DbUtil.getTableName;
import static org.folio.tag.repository.DbUtil.mapVertxFuture;
import static org.folio.tag.repository.providers.ProviderTableConstants.DELETE_PROVIDER_STATEMENT;
import static org.folio.tag.repository.providers.ProviderTableConstants.INSERT_OR_UPDATE_PROVIDER_STATEMENT;
import static org.folio.tag.repository.providers.ProviderTableConstants.PROVIDERS_TABLE_NAME;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.persist.PostgresClient;
import org.folio.tag.repository.DbUtil;

@Component
public class ProviderRepositoryImpl implements ProviderRepository {

  private static final Logger LOG = LoggerFactory.getLogger(ProviderRepositoryImpl.class);

  private Vertx vertx;

  @Autowired
  public ProviderRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<Void> saveProvider(VendorById vendorData, String tenantId) {
    JsonArray parameters = DbUtil.createInsertOrUpdateParameters(String.valueOf(vendorData.getVendorId()),
      vendorData.getVendorName());

    final String query = String.format(INSERT_OR_UPDATE_PROVIDER_STATEMENT,
      getTableName(tenantId, PROVIDERS_TABLE_NAME));
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do insert query = " + query);
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, parameters, future.completer());
    return mapVertxFuture(future).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> deleteProvider(String vendorId, String tenantId) {
    JsonArray parameter = new JsonArray(Collections.singletonList(vendorId));

    final String query = String.format(DELETE_PROVIDER_STATEMENT, getTableName(tenantId, PROVIDERS_TABLE_NAME));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do delete query = " + query);
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, parameter, future.completer());
    return mapVertxFuture(future).thenApply(result -> null);
  }
}
