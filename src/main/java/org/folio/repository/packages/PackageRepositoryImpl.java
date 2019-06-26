package org.folio.repository.packages;

import static java.util.stream.Collectors.groupingBy;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.FutureUtils.mapVertxFuture;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.DbUtil.getPackagesTableName;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.packages.PackageTableConstants.CONTENT_TYPE_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.DELETE_STATEMENT;
import static org.folio.repository.packages.PackageTableConstants.ID_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.INSERT_OR_UPDATE_STATEMENT;
import static org.folio.repository.packages.PackageTableConstants.NAME_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.SELECT_PACKAGES_WITH_TAGS;
import static org.folio.repository.packages.PackageTableConstants.SELECT_PACKAGES_WITH_TAGS_BY_IDS;
import static org.folio.repository.packages.PackageTableConstants.SELECT_PACKAGE_IDS_BY_TAG;
import static org.folio.repository.tag.TagTableConstants.TAG_COLUMN;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import org.apache.commons.collections4.CollectionUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
  public CompletableFuture<Void> save(DbPackage packageData, String tenantId){
    JsonArray parameters = createInsertOrUpdateParameters(
      packageIdToString(packageData.getId()) , packageData.getName(), packageData.getContentType());

    final String query = String.format(INSERT_OR_UPDATE_STATEMENT, getPackagesTableName(tenantId));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do insert query = " + query);
    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, parameters, future.completer());
    return mapVertxFuture(future)
      .thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> delete(PackageId packageId, String tenantId) {
    JsonArray parameter = new JsonArray(Collections.singletonList(packageIdToString(packageId)));

    final String query = String.format(DELETE_STATEMENT, getPackagesTableName(tenantId));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do delete query = " + query);

    Future<UpdateResult> future = Future.future();
    postgresClient.execute(query, parameter, future.completer());
    return mapVertxFuture(future)
      .thenApply(result -> null);
  }

  @Override
  public CompletableFuture<List<DbPackage>> findByTagName(List<String> tags, int page, int count, String tenantId) {
    return getPackageIdsByTagAndIdPrefix(tags, "", page, count, tenantId);
  }

  @Override
  public CompletableFuture<List<DbPackage>> findByTagNameAndProvider(List<String> tags, String providerId, int page, int count, String tenantId) {
    return getPackageIdsByTagAndIdPrefix(tags, providerId + "-", page, count, tenantId);
  }

  @Override
  public CompletableFuture<List<DbPackage>> findAllById(List<PackageId> packageIds, String tenantId) {
    if(CollectionUtils.isEmpty(packageIds)){
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
    JsonArray parameters = new JsonArray();
    packageIds.forEach(packageId -> parameters.add(packageIdToString(packageId)));

    final String query = String.format(SELECT_PACKAGES_WITH_TAGS_BY_IDS, getPackagesTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(packageIds.size()));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Select packages by ids = " + query);
    Future<ResultSet> future = Future.future();
    postgresClient.select(query, parameters, future.completer());

    return mapResult(future, this::mapPackages);
  }

  private CompletableFuture<List<DbPackage>> getPackageIdsByTagAndIdPrefix(List<String> tags, String prefix, int page, int count, String tenantId) {
    if(CollectionUtils.isEmpty(tags)){
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
    int offset = (page - 1) * count;

    JsonArray parameters = new JsonArray();
    tags.forEach(parameters::add);
    String likeExpression = prefix + "%";
    parameters
      .add(likeExpression)
      .add(offset)
      .add(count);

    final String resourceIdsQuery = String.format(SELECT_PACKAGE_IDS_BY_TAG, getPackagesTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(tags.size()));

    final String query = String.format(SELECT_PACKAGES_WITH_TAGS, getPackagesTableName(tenantId),
      getTagsTableName(tenantId), resourceIdsQuery);

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Select packages by tags = " + query);
    Future<ResultSet> future = Future.future();
    postgresClient.select(query, parameters, future.completer());

    return mapResult(future, this::mapPackages);
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

  private List<DbPackage> mapPackages(ResultSet resultSet) {
    Map<PackageId, List<JsonObject>> rowsById = resultSet.getRows().stream()
      .collect(groupingBy(this::readPackageId));
    return mapItems(rowsById.entrySet(), this::readPackage);
  }

  private PackageId readPackageId(JsonObject row) {
    return idParser.parsePackageId(row.getString(ID_COLUMN));
  }

  private DbPackage readPackage(Map.Entry<PackageId, List<JsonObject>> entry) {
    PackageId packageId = entry.getKey();
    List<JsonObject> rows = entry.getValue();

    JsonObject firstRow = rows.get(0);
    List<String> tags = rows.stream()
      .map(row -> row.getString(TAG_COLUMN))
      .collect(Collectors.toList());
    return new DbPackage.DbPackageBuilder()
      .id(packageId)
      .contentType(firstRow.getString(CONTENT_TYPE_COLUMN))
      .name(firstRow.getString(NAME_COLUMN))
      .tags(tags)
      .build();
  }

  private String packageIdToString(PackageId packageId) {
    return packageId.getProviderIdPart() + "-" + packageId.getPackageIdPart();
  }
}
