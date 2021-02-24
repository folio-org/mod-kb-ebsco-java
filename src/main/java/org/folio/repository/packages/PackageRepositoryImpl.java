package org.folio.repository.packages;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.common.LogUtils.logDeleteQueryInfoLevel;
import static org.folio.common.LogUtils.logInsertQueryInfoLevel;
import static org.folio.common.LogUtils.logSelectQueryInfoLevel;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.getPackagesTableName;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.DbUtil.pgClient;
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
import static org.folio.rest.util.IdParser.parsePackageId;
import static org.folio.util.FutureUtils.mapResult;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.db.RowSetUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.holdingsiq.model.PackageId;
import org.folio.rest.model.filter.TagFilter;
import org.folio.rest.util.IdParser;

@Component
public class PackageRepositoryImpl implements PackageRepository {

  private static final Logger LOG = LogManager.getLogger(PackageRepositoryImpl.class);

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

    logInsertQueryInfoLevel(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId, vertx).execute(query, parameters, promise);
    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Void> delete(PackageId packageId, UUID credentialsId, String tenantId) {
    Tuple parameters = createParams(asList(IdParser.packageIdToString(packageId), credentialsId));

    final String query = prepareQuery(DELETE_STATEMENT, getPackagesTableName(tenantId));

    logDeleteQueryInfoLevel(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId, vertx).execute(query, parameters, promise);
    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<List<DbPackage>> findByTagFilter(TagFilter tagFilter, UUID credentialsId, String tenantId) {
    return getPackageIdsByTagAndIdPrefix(tagFilter, credentialsId, tenantId);
  }

  @Override
  public CompletableFuture<List<DbPackage>> findByIds(List<PackageId> packageIds, UUID credentialsId,
                                                      String tenantId) {
    if (CollectionUtils.isEmpty(packageIds)) {
      return completedFuture(Collections.emptyList());
    }

    Tuple parameters = Tuple.tuple();
    packageIds.forEach(packageId -> parameters.addString(IdParser.packageIdToString(packageId)));
    parameters.addUUID(credentialsId);

    final String query = prepareQuery(SELECT_PACKAGES_WITH_TAGS_BY_IDS, getPackagesTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(packageIds.size()));

    logSelectQueryInfoLevel(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId, vertx).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapPackages);
  }

  private CompletableFuture<List<DbPackage>> getPackageIdsByTagAndIdPrefix(TagFilter tagFilter, UUID credentialsId,
                                                                           String tenantId) {
    List<String> tags = tagFilter.getTags();
    if (CollectionUtils.isEmpty(tags)) {
      return completedFuture(Collections.emptyList());
    }

    Tuple parameters = Tuple.tuple();
    parameters
      .addString(tagFilter.getRecordIdPrefix() + "%")
      .addUUID(credentialsId);
    tags.forEach(parameters::addString);
    parameters
      .addInteger(tagFilter.getOffset())
      .addInteger(tagFilter.getCount());

    final String query = prepareQuery(SELECT_PACKAGES_WITH_TAGS, getPackagesTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(tags.size()));

    logSelectQueryInfoLevel(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId, vertx).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapPackages);
  }

  private Tuple createInsertOrUpdateParameters(String id, UUID credentialsId, String name, String contentType) {
    return createParams(asList(id, credentialsId, name, contentType, name, contentType));
  }

  private List<DbPackage> mapPackages(RowSet<Row> resultSet) {
    return RowSetUtils.mapItems(resultSet, entry -> DbPackage.builder()
      .id(parsePackageId(entry.getString(ID_COLUMN)))
      .credentialsId(entry.getUUID(CREDENTIALS_ID_COLUMN))
      .contentType(entry.getString(CONTENT_TYPE_COLUMN))
      .name(entry.getString(NAME_COLUMN))
      .tags(asList(entry.getArrayOfStrings(TAG_COLUMN)))
      .build()
    );
  }

}
