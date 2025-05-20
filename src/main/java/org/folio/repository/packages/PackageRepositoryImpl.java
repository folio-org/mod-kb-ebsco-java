package org.folio.repository.packages;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.LogUtils.logDeleteQuery;
import static org.folio.common.LogUtils.logInsertQuery;
import static org.folio.common.LogUtils.logSelectQuery;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.pgClient;
import static org.folio.repository.packages.PackageTableConstants.CONTENT_TYPE_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.ID_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.NAME_COLUMN;
import static org.folio.repository.packages.PackageTableConstants.deleteStatement;
import static org.folio.repository.packages.PackageTableConstants.insertOrUpdateStatement;
import static org.folio.repository.packages.PackageTableConstants.selectPackagesWithTags;
import static org.folio.repository.packages.PackageTableConstants.selectPackagesWithTagsByIds;
import static org.folio.repository.tag.TagTableConstants.TAG_COLUMN;
import static org.folio.rest.util.IdParser.parsePackageId;
import static org.folio.util.FutureUtils.mapResult;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.db.RowSetUtils;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.holdingsiq.model.PackageId;
import org.folio.rest.model.filter.TagFilter;
import org.folio.rest.util.IdParser;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class PackageRepositoryImpl implements PackageRepository {
  private final Vertx vertx;
  private final DBExceptionTranslator excTranslator;

  public PackageRepositoryImpl(Vertx vertx, DBExceptionTranslator excTranslator) {
    this.vertx = vertx;
    this.excTranslator = excTranslator;
  }

  @Override
  public CompletableFuture<Void> save(DbPackage packageData, String tenantId) {
    Tuple parameters = createInsertOrUpdateParameters(
      IdParser.packageIdToString(packageData.getId()),
      packageData.getCredentialsId(),
      packageData.getName(),
      packageData.getContentType()
    );

    final String query = insertOrUpdateStatement(tenantId);

    logInsertQuery(log, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId, vertx).execute(query, parameters, promise);
    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Void> delete(PackageId packageId, UUID credentialsId, String tenantId) {
    Tuple parameters = createParams(asList(IdParser.packageIdToString(packageId), credentialsId));

    final String query = deleteStatement(tenantId);

    logDeleteQuery(log, query, parameters);

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

    final String query = selectPackagesWithTagsByIds(tenantId, packageIds);

    logSelectQuery(log, query, parameters);

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

    final String query = selectPackagesWithTags(tenantId, tags);

    logSelectQuery(log, query, parameters);

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
