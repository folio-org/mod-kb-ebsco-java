package org.folio.repository.resources;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.common.LogUtils.logDeleteQueryInfoLevel;
import static org.folio.common.LogUtils.logInsertQueryInfoLevel;
import static org.folio.common.LogUtils.logSelectQueryInfoLevel;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.getResourcesTableName;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.DbUtil.pgClient;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.resources.ResourceTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.resources.ResourceTableConstants.DELETE_RESOURCE_STATEMENT;
import static org.folio.repository.resources.ResourceTableConstants.INSERT_OR_UPDATE_RESOURCE_STATEMENT;
import static org.folio.repository.resources.ResourceTableConstants.SELECT_RESOURCES_WITH_TAGS;
import static org.folio.repository.tag.TagTableConstants.TAG_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.ID_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.NAME_COLUMN;
import static org.folio.rest.util.IdParser.parseResourceId;
import static org.folio.rest.util.IdParser.resourceIdToString;
import static org.folio.util.FutureUtils.mapResult;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
import org.folio.rest.model.filter.TagFilter;

@Component
public class ResourceRepositoryImpl implements ResourceRepository {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceRepositoryImpl.class);

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public CompletableFuture<Void> save(DbResource resource, String tenantId) {
    Tuple parameters = createParams(asList(
      resourceIdToString(resource.getId()),
      resource.getCredentialsId(),
      resource.getName(),
      resource.getName()
    ));

    final String query = prepareQuery(INSERT_OR_UPDATE_RESOURCE_STATEMENT, getResourcesTableName(tenantId));

    logInsertQueryInfoLevel(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId, vertx).execute(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Void> delete(String resourceId, UUID credentialsId, String tenantId) {
    Tuple params = createParams(resourceId, credentialsId);

    final String query = prepareQuery(DELETE_RESOURCE_STATEMENT, getResourcesTableName(tenantId));

    logDeleteQueryInfoLevel(LOG, query, params);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId, vertx).execute(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<List<DbResource>> findByTagFilter(TagFilter tagFilter, UUID credentialsId, String tenantId) {
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

    final String query = prepareQuery(SELECT_RESOURCES_WITH_TAGS, getResourcesTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(tags.size()));

    logSelectQueryInfoLevel(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId, vertx).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapResources);
  }

  private List<DbResource> mapResources(RowSet<Row> resultSet) {
    return RowSetUtils.mapItems(resultSet, entry -> DbResource.builder()
      .id(parseResourceId(entry.getString(ID_COLUMN)))
      .credentialsId(entry.getUUID(CREDENTIALS_ID_COLUMN))
      .name(entry.getString(NAME_COLUMN))
      .tags(asList(entry.getStringArray(TAG_COLUMN)))
      .build()
    );
  }

}
