package org.folio.repository.resources;

import static java.util.stream.Collectors.groupingBy;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.FutureUtils.mapVertxFuture;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.DbUtil.createInsertOrUpdateParameters;
import static org.folio.repository.DbUtil.getResourcesTableName;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.resources.ResourceTableConstants.DELETE_RESOURCE_STATEMENT;
import static org.folio.repository.resources.ResourceTableConstants.INSERT_OR_UPDATE_RESOURCE_STATEMENT;
import static org.folio.repository.resources.ResourceTableConstants.SELECT_RESOURCES_WITH_TAGS;
import static org.folio.repository.resources.ResourceTableConstants.SELECT_RESOURCE_IDS_BY_TAG;
import static org.folio.repository.tag.TagTableConstants.TAG_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.ID_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.NAME_COLUMN;

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

import org.folio.holdingsiq.model.ResourceId;
import org.folio.rest.parser.IdParser;
import org.folio.rest.persist.PostgresClient;

@Component
public class ResourceRepositoryImpl implements ResourceRepository {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceRepositoryImpl.class);
  private IdParser idParser;
  private Vertx vertx;

  @Autowired
  public ResourceRepositoryImpl(Vertx vertx, IdParser idParser) {
    this.vertx = vertx;
    this.idParser = idParser;
  }

  @Override
  public CompletableFuture<Void> save(String resourceId, String name, String tenantId) {

    JsonArray parameters = createInsertOrUpdateParameters(resourceId, name);

    final String query = String.format(INSERT_OR_UPDATE_RESOURCE_STATEMENT, getResourcesTableName(tenantId));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do insert query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    postgresClient.execute(query, parameters, promise);
    return mapVertxFuture(promise.future()).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> delete(String resourceId, String tenantId) {
    JsonArray parameter = new JsonArray(Collections.singletonList(resourceId));

    final String query = String.format(DELETE_RESOURCE_STATEMENT, getResourcesTableName(tenantId));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do delete query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    postgresClient.execute(query, parameter, promise);
    return mapVertxFuture(promise.future()).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<List<ResourceInfoInDB>> findByTagNameAndPackageId(List<String> tags, String resourceId, int page, int count, String tenantId) {
    if(CollectionUtils.isEmpty(tags)){
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
    int offset = (page - 1) * count;

    JsonArray parameters = new JsonArray();
    tags.forEach(parameters::add);
    String likeExpression = resourceId + "-%";
    parameters
      .add(likeExpression)
      .add(offset)
      .add(count);

    final String resourceIdsQuery = String.format(SELECT_RESOURCE_IDS_BY_TAG, getResourcesTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(tags.size()));

    final String query = String.format(SELECT_RESOURCES_WITH_TAGS, getResourcesTableName(tenantId),
      getTagsTableName(tenantId), resourceIdsQuery);

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Select resources by tags = " + query);
    Promise<ResultSet> promise = Promise.promise();
    postgresClient.select(query, parameters, promise);

    return mapResult(promise.future(), this::mapResources);
  }

  private List<ResourceInfoInDB> mapResources(ResultSet resultSet) {
    final Map<ResourceId, List<JsonObject>> rowsById = resultSet.getRows().stream()
      .collect(groupingBy(this::readResourceId));
    return mapItems(rowsById.entrySet(), this::readResource);
  }
  private ResourceId readResourceId(JsonObject row) {
    return idParser.parseResourceId(row.getString(ID_COLUMN));
  }

  private ResourceInfoInDB readResource(Map.Entry<ResourceId, List<JsonObject>> entry) {
    ResourceId resourceId = entry.getKey();
    List<JsonObject> rows = entry.getValue();

    JsonObject firstRow = rows.get(0);
    List<String> tags = rows.stream()
      .map(row -> row.getString(TAG_COLUMN))
      .collect(Collectors.toList());

    return ResourceInfoInDB.builder()
      .id(resourceId)
      .name(firstRow.getString(NAME_COLUMN))
      .tags(tags)
      .build();
  }
}
