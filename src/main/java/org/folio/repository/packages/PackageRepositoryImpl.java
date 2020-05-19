package org.folio.repository.packages;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.groupingBy;

import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.DELETE_LOG_MESSAGE;
import static org.folio.repository.DbUtil.SELECT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.getPackagesTableName;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.packages.PackageTableConstants.CONTENT_TYPE_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.DELETE_STATEMENT;
import static org.folio.repository.packages.PackageTableConstants.ID_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.INSERT_OR_UPDATE_STATEMENT;
import static org.folio.repository.packages.PackageTableConstants.NAME_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.SELECT_PACKAGES_WITH_TAGS;
import static org.folio.repository.packages.PackageTableConstants.SELECT_PACKAGES_WITH_TAGS_BY_IDS;
import static org.folio.repository.tag.TagTableConstants.TAG_COLUMN;
import static org.folio.util.FutureUtils.mapResult;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.Promise;
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
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.util.IdParser;

@Component
public class PackageRepositoryImpl implements PackageRepository {

  private static final Logger LOG = LoggerFactory.getLogger(PackageRepositoryImpl.class);

  @Autowired
  private Vertx vertx;

  @Override
  public CompletableFuture<Void> save(PackageInfoInDB packageData, String credentialsId, String tenantId){
    JsonArray parameters = createInsertOrUpdateParameters(IdParser.packageIdToString(packageData.getId()),
      credentialsId, packageData.getName(), packageData.getContentType());

    final String query = String.format(INSERT_OR_UPDATE_STATEMENT, getPackagesTableName(tenantId));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<UpdateResult> promise = Promise.promise();
    postgresClient.execute(query, parameters, promise);
    return mapVertxFuture(promise.future())
      .thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> delete(PackageId packageId, String credentialsId, String tenantId) {
    JsonArray parameter = createParams(asList(IdParser.packageIdToString(packageId), credentialsId));

    final String query = String.format(DELETE_STATEMENT, getPackagesTableName(tenantId));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info(DELETE_LOG_MESSAGE, query);

    Promise<UpdateResult> promise = Promise.promise();
    postgresClient.execute(query, parameter, promise);
    return mapVertxFuture(promise.future())
      .thenApply(result -> null);
  }

  @Override
  public CompletableFuture<List<PackageInfoInDB>> findByTagName(List<String> tags, int page, int count,
      String credentialsId, String tenantId) {
    return getPackageIdsByTagAndIdPrefix(tags, "", page, count, credentialsId, tenantId);
  }

  @Override
  public CompletableFuture<List<PackageInfoInDB>> findByTagNameAndProvider(List<String> tags, String providerId,
      int page, int count, String credentialsId, String tenantId) {
    return getPackageIdsByTagAndIdPrefix(tags, providerId + "-", page, count, credentialsId, tenantId);
  }

  @Override
  public CompletableFuture<List<PackageInfoInDB>> findByIds(List<PackageId> packageIds, String credentialsId,
      String tenantId) {
    if(CollectionUtils.isEmpty(packageIds)){
      return completedFuture(Collections.emptyList());
    }

    JsonArray parameters = new JsonArray();
    packageIds.forEach(packageId -> parameters.add(IdParser.packageIdToString(packageId)));
    parameters.add(credentialsId);

    final String query = String.format(SELECT_PACKAGES_WITH_TAGS_BY_IDS, getPackagesTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(packageIds.size()));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<ResultSet> promise = Promise.promise();
    postgresClient.select(query, parameters, promise);

    return mapResult(promise.future(), this::mapPackages);
  }

  private CompletableFuture<List<PackageInfoInDB>> getPackageIdsByTagAndIdPrefix(List<String> tags, String prefix,
      int page, int count, String credentialsId, String tenantId) {
    if(CollectionUtils.isEmpty(tags)){
      return completedFuture(Collections.emptyList());
    }
    int offset = (page - 1) * count;

    JsonArray parameters = new JsonArray();
    tags.forEach(parameters::add);
    String likeExpression = prefix + "%";
    parameters
      .add(likeExpression)
      .add(credentialsId)
      .add(offset)
      .add(count);

    final String query = String.format(SELECT_PACKAGES_WITH_TAGS, getPackagesTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(tags.size()));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<ResultSet> promise = Promise.promise();
    postgresClient.select(query, parameters, promise);

    return mapResult(promise.future(), this::mapPackages);
  }

  private JsonArray createInsertOrUpdateParameters(String id, String credentialsId, String name, String contentType) {
    return createParams(asList(id, credentialsId, name, contentType, name, contentType));
  }

  private List<PackageInfoInDB> mapPackages(ResultSet resultSet) {
    Map<PackageId, List<JsonObject>> rowsById = resultSet.getRows().stream()
      .collect(groupingBy(this::readPackageId));
    return mapItems(rowsById.entrySet(), this::readPackage);
  }

  private PackageId readPackageId(JsonObject row) {
    return IdParser.parsePackageId(row.getString(ID_COLUMN));
  }

  private PackageInfoInDB readPackage(Map.Entry<PackageId, List<JsonObject>> entry) {
    PackageId packageId = entry.getKey();
    List<JsonObject> rows = entry.getValue();

    JsonObject firstRow = rows.get(0);
    List<String> tags = rows.stream()
      .map(row -> row.getString(TAG_COLUMN))
      .collect(Collectors.toList());
    return new PackageInfoInDB.PackageInfoInDBBuilder()
      .id(packageId)
      .contentType(firstRow.getString(CONTENT_TYPE_COLUMN))
      .name(firstRow.getString(NAME_COLUMN))
      .tags(tags)
      .build();
  }

}
