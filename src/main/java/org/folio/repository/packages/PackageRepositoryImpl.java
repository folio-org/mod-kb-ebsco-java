package org.folio.repository.packages;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.groupingBy;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.DELETE_LOG_MESSAGE;
import static org.folio.repository.DbUtil.INSERT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.SELECT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.getPackagesTableName;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.packages.PackageTableConstants.CONTENT_TYPE_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.DELETE_STATEMENT;
import static org.folio.repository.packages.PackageTableConstants.ID_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.INSERT_OR_UPDATE_STATEMENT;
import static org.folio.repository.packages.PackageTableConstants.NAME_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.SELECT_PACKAGES_WITH_TAGS;
import static org.folio.repository.packages.PackageTableConstants.SELECT_PACKAGES_WITH_TAGS_BY_IDS;
import static org.folio.repository.tag.TagTableConstants.TAG_COLUMN;
import static org.folio.util.FutureUtils.mapResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.RowSetUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.holdingsiq.model.PackageId;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.util.IdParser;

@Component
public class PackageRepositoryImpl implements PackageRepository {

  private static final Logger LOG = LoggerFactory.getLogger(PackageRepositoryImpl.class);

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public CompletableFuture<Void> save(DbPackage packageData, String tenantId) {
    Tuple parameters = createInsertOrUpdateParameters(
      IdParser.packageIdToString(packageData.getId()),
      packageData.getCredentialsId(),
      packageData.getName(),
      packageData.getContentType()
    );

    final String query = prepareQuery(INSERT_OR_UPDATE_STATEMENT, getPackagesTableName(tenantId));

    LOG.info(INSERT_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);
    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Void> delete(PackageId packageId, String credentialsId, String tenantId) {
    Tuple parameters = createParams(asList(IdParser.packageIdToString(packageId), credentialsId));

    final String query = prepareQuery(DELETE_STATEMENT, getPackagesTableName(tenantId));

    LOG.info(DELETE_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);
    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<List<DbPackage>> findByTagName(List<String> tags, int page, int count,
                                                          String credentialsId, String tenantId) {
    return getPackageIdsByTagAndIdPrefix(tags, "", page, count, credentialsId, tenantId);
  }

  @Override
  public CompletableFuture<List<DbPackage>> findByTagNameAndProvider(List<String> tags, String providerId,
                                                                     int page, int count, String credentialsId,
                                                                     String tenantId) {
    return getPackageIdsByTagAndIdPrefix(tags, providerId + "-", page, count, credentialsId, tenantId);
  }

  @Override
  public CompletableFuture<List<DbPackage>> findByIds(List<PackageId> packageIds, String credentialsId,
                                                      String tenantId) {
    if (CollectionUtils.isEmpty(packageIds)) {
      return completedFuture(Collections.emptyList());
    }

    Tuple parameters = Tuple.tuple();
    packageIds.forEach(packageId -> parameters.addString(IdParser.packageIdToString(packageId)));
    parameters.addString(credentialsId);

    final String query = prepareQuery(SELECT_PACKAGES_WITH_TAGS_BY_IDS, getPackagesTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(packageIds.size()));

    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapPackages);
  }

  private CompletableFuture<List<DbPackage>> getPackageIdsByTagAndIdPrefix(List<String> tags, String prefix,
                                                                           int page, int count, String credentialsId,
                                                                           String tenantId) {
    if (CollectionUtils.isEmpty(tags)) {
      return completedFuture(Collections.emptyList());
    }
    int offset = (page - 1) * count;

    Tuple parameters = Tuple.tuple();
    tags.forEach(parameters::addString);
    String likeExpression = prefix + "%";
    parameters
      .addString(likeExpression)
      .addString(credentialsId)
      .addInteger(offset)
      .addInteger(count);

    final String query = prepareQuery(SELECT_PACKAGES_WITH_TAGS, getPackagesTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(tags.size()));

    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapPackages);
  }

  private Tuple createInsertOrUpdateParameters(String id, String credentialsId, String name, String contentType) {
    return createParams(asList(id, credentialsId, name, contentType, name, contentType));
  }

  private List<DbPackage> mapPackages(RowSet<Row> resultSet) {
    Map<PackageId, List<Row>> rowsById = RowSetUtils.streamOf(resultSet)
      .collect(groupingBy(this::readPackageId));
    return mapItems(rowsById.entrySet(), this::readPackage);
  }

  private PackageId readPackageId(Row row) {
    return IdParser.parsePackageId(row.getString(ID_COLUMN));
  }

  private DbPackage readPackage(Map.Entry<PackageId, List<Row>> entry) {
    PackageId packageId = entry.getKey();
    List<Row> rows = entry.getValue();

    Row firstRow = rows.get(0);
    List<String> tags = rows.stream()
      .map(row -> row.getString(TAG_COLUMN))
      .collect(Collectors.toList());
    return DbPackage.builder()
      .id(packageId)
      .credentialsId(firstRow.getString(CREDENTIALS_ID_COLUMN))
      .contentType(firstRow.getString(CONTENT_TYPE_COLUMN))
      .name(firstRow.getString(NAME_COLUMN))
      .tags(tags)
      .build();
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }

}
