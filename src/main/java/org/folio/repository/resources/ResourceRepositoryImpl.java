package org.folio.repository.resources;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.groupingBy;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.common.LogUtils.logDeleteQuery;
import static org.folio.common.LogUtils.logInsertQuery;
import static org.folio.common.LogUtils.logSelectQuery;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.getResourcesTableName;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.resources.ResourceTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.resources.ResourceTableConstants.DELETE_RESOURCE_STATEMENT;
import static org.folio.repository.resources.ResourceTableConstants.INSERT_OR_UPDATE_RESOURCE_STATEMENT;
import static org.folio.repository.resources.ResourceTableConstants.SELECT_RESOURCES_WITH_TAGS;
import static org.folio.repository.tag.TagTableConstants.TAG_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.ID_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.NAME_COLUMN;
import static org.folio.rest.util.IdParser.resourceIdToString;
import static org.folio.util.FutureUtils.mapResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.folio.holdingsiq.model.ResourceId;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.util.IdParser;

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

    logInsertQuery(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Void> delete(String resourceId, UUID credentialsId, String tenantId) {
    Tuple params = createParams(resourceId, credentialsId);

    final String query = prepareQuery(DELETE_RESOURCE_STATEMENT, getResourcesTableName(tenantId));

    logDeleteQuery(LOG, query, params);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<List<DbResource>> findByTagNameAndPackageId(List<String> tags, String resourceId,
                                                                       int page, int count, UUID credentialsId,
                                                                       String tenantId) {
    if (CollectionUtils.isEmpty(tags)) {
      return completedFuture(Collections.emptyList());
    }

    int offset = (page - 1) * count;

    Tuple parameters = Tuple.tuple();
    tags.forEach(parameters::addString);
    String likeExpression = resourceId + "-%";
    parameters
      .addString(likeExpression)
      .addUUID(credentialsId)
      .addInteger(offset)
      .addInteger(count);

    final String query = prepareQuery(SELECT_RESOURCES_WITH_TAGS, getResourcesTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(tags.size()));

    logSelectQuery(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapResources);
  }

  private List<DbResource> mapResources(RowSet<Row> resultSet) {
    final Map<ResourceId, List<Row>> rowsById = RowSetUtils.streamOf(resultSet)
      .collect(groupingBy(this::readResourceId));
    return mapItems(rowsById.entrySet(), this::readResource);
  }

  private ResourceId readResourceId(Row row) {
    return IdParser.parseResourceId(row.getString(ID_COLUMN));
  }

  private DbResource readResource(Map.Entry<ResourceId, List<Row>> entry) {
    ResourceId resourceId = entry.getKey();
    List<Row> rows = entry.getValue();

    Row firstRow = rows.get(0);
    List<String> tags = rows.stream()
      .map(row -> row.getString(TAG_COLUMN))
      .collect(Collectors.toList());

    return DbResource.builder()
      .id(resourceId)
      .credentialsId(firstRow.getUUID(CREDENTIALS_ID_COLUMN))
      .name(firstRow.getString(NAME_COLUMN))
      .tags(tags)
      .build();
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
