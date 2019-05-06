package org.folio.tag.repository.providers;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.UpdateResult;
import org.folio.holdingsiq.model.VendorById;
import org.folio.rest.persist.PostgresClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.folio.tag.repository.providers.ProviderTableConstants.DELETE_PROVIDER_STATEMENT;
import static org.folio.tag.repository.providers.ProviderTableConstants.INSERT_OR_UPDATE_PROVIDER_STATEMENT;
import static org.folio.tag.repository.providers.ProviderTableConstants.PROVIDERS_TABLE_NAME;

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
    JsonArray parameters = insertOrUpdateProviderParameters(vendorData.getVendorId(), vendorData.getVendorName());

    final String query = String.format(INSERT_OR_UPDATE_PROVIDER_STATEMENT, getTableName(tenantId));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do insert query = " + query);
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, parameters, future.completer());
    return mapVertxFuture(future).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> deleteProvider(String vendorId, String tenantId) {
    JsonArray parameter = new JsonArray(Collections.singletonList(vendorId));

    final String query = String.format(DELETE_PROVIDER_STATEMENT, getTableName(tenantId));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do delete query = " + query);
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, parameter, future.completer());
    return mapVertxFuture(future).thenApply(result -> null);
  }

  private JsonArray insertOrUpdateProviderParameters(long id, String name) {
    return new JsonArray()
      .add(id)
      .add(name)
      .add(name);
  }

  private String getTableName(String tenantId) {
    return PostgresClient.convertToPsqlStandard(tenantId) + "." + PROVIDERS_TABLE_NAME;
  }

  private <T> CompletableFuture<T> mapVertxFuture(Future<T> future) {
    CompletableFuture<T> completableFuture = new CompletableFuture<>();

    future.map(completableFuture::complete).otherwise(completableFuture::completeExceptionally);
    return completableFuture;
  }
}
