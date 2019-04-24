package org.folio.tag.repository.packages;

import static org.folio.tag.repository.packages.PackageTableConstants.INSERT_OR_UPDATE_STATEMENT;
import static org.folio.tag.repository.packages.PackageTableConstants.TABLE_NAME;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.UpdateResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.rest.persist.PostgresClient;

@Component
public class PackageRepositoryImpl implements PackageRepository {

  private static final Logger LOG = LoggerFactory.getLogger(PackageRepositoryImpl.class);
  private Vertx vertx;

  @Autowired
  public PackageRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<Void> savePackage(PackageByIdData packageData, String tenantId){
    String fullPackageId = packageData.getVendorId() + "-" + packageData.getPackageId();
    JsonArray parameters = createInsertOrUpdateParameters(
      fullPackageId, packageData.getPackageName(), packageData.getContentType());

    final String query = String.format(INSERT_OR_UPDATE_STATEMENT, getTableName(tenantId));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do insert query = " + query);
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, parameters, future.completer());
    return mapVertxFuture(future)
      .thenApply(result -> null);
  }

  private JsonArray createInsertOrUpdateParameters(String id, String name, String contentType) {
    JsonArray parameters = new JsonArray();
    parameters
      .add(id)
      .add(name)
      .add(contentType)
      .add(name)
      .add(contentType);
    return parameters;
  }

  private String getTableName(String tenantId) {
    return PostgresClient.convertToPsqlStandard(tenantId) + "." + TABLE_NAME;
  }

  private <T> CompletableFuture<T> mapVertxFuture(Future<T> future){
    CompletableFuture<T> completableFuture = new CompletableFuture<>();

    future
      .map(completableFuture::complete)
      .otherwise(completableFuture::completeExceptionally);

    return completableFuture;
  }
}
