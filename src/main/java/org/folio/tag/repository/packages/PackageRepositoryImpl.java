package org.folio.tag.repository.packages;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.tag.repository.DbUtil.mapResultSet;
import static org.folio.tag.repository.DbUtil.mapVertxFuture;
import static org.folio.tag.repository.packages.PackageTableConstants.DELETE_STATEMENT;
import static org.folio.tag.repository.packages.PackageTableConstants.INSERT_OR_UPDATE_STATEMENT;
import static org.folio.tag.repository.packages.PackageTableConstants.SELECT_TAGGED_PACKAGES;
import static org.folio.tag.repository.packages.PackageTableConstants.TABLE_NAME;

import java.util.Collections;
import java.util.List;
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

import org.folio.holdingsiq.model.PackageByIdData;
import org.folio.holdingsiq.model.PackageId;
import org.folio.rest.parser.IdParser;
import org.folio.rest.persist.PostgresClient;

@Component
public class PackageRepositoryImpl implements PackageRepository {

  private static final Logger LOG = LoggerFactory.getLogger(PackageRepositoryImpl.class);
  private IdParser idParser;
  private Vertx vertx;

  @Autowired
  public PackageRepositoryImpl(Vertx vertx, IdParser idParser) {
    this.vertx = vertx;
    this.idParser = idParser;
  }

  @Override
  public CompletableFuture<Void> savePackage(PackageByIdData packageData, String tenantId) {
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

  @Override
  public CompletableFuture<Void> deletePackage(PackageId packageId, String tenantId) {
    JsonArray parameter = new JsonArray(Collections.singletonList(packageId.getProviderIdPart() + "-" + packageId.getPackageIdPart()));

    final String query = String.format(DELETE_STATEMENT, getTableName(tenantId));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do delete query = " + query);

    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, parameter, future.completer());
    return mapVertxFuture(future)
      .thenApply(result -> null);
  }

  @Override
  public CompletableFuture<List<PackageId>> getPackageIdsByTagName(List<String> tags, int page, int count, String tenantId) {
    int offset = (page - 1) * count;

    JsonArray parameters = new JsonArray();
    tags.forEach(parameters::add);
    parameters
      .add(offset)
      .add(count);

    final String query = String.format(SELECT_TAGGED_PACKAGES, getTableName(tenantId), createPlaceholders(tags.size()));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Select packages by tags = " + query);
    Future<ResultSet> future = Future.future();
    postgresClient.select(query, parameters, future.completer());

    return mapResultSet(future, this::mapPackageIds);
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

  private String createPlaceholders(int size) {
    return String.join(",", Collections.nCopies(size, "?"));
  }

  private List<PackageId> mapPackageIds(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), this::readPackageId);
  }

  private PackageId readPackageId(JsonObject row) {
    return idParser.parsePackageId(row.getString("id"));
  }
}
