package org.folio.repository.resources;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.LogUtils.logDeleteQuery;
import static org.folio.common.LogUtils.logInsertQuery;
import static org.folio.common.LogUtils.logSelectQuery;
import static org.folio.db.DbUtils.createParams;
import static org.folio.repository.DbUtil.pgClient;
import static org.folio.repository.resources.ResourceTableConstants.CREDENTIALS_ID_COLUMN;
import static org.folio.repository.resources.ResourceTableConstants.deleteResourceStatement;
import static org.folio.repository.resources.ResourceTableConstants.insertOrUpdateResourceStatement;
import static org.folio.repository.resources.ResourceTableConstants.selectResourcesWithTags;
import static org.folio.repository.tag.TagTableConstants.TAG_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.ID_COLUMN;
import static org.folio.repository.titles.TitlesTableConstants.NAME_COLUMN;
import static org.folio.rest.util.IdParser.parseResourceId;
import static org.folio.rest.util.IdParser.resourceIdToString;
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
import org.folio.rest.model.filter.TagFilter;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class ResourceRepositoryImpl implements ResourceRepository {

  private final Vertx vertx;
  private final DBExceptionTranslator excTranslator;

  public ResourceRepositoryImpl(Vertx vertx, DBExceptionTranslator excTranslator) {
    this.vertx = vertx;
    this.excTranslator = excTranslator;
  }

  @Override
  public CompletableFuture<Void> save(DbResource resource, String tenantId) {
    Tuple parameters = createParams(asList(
      resourceIdToString(resource.getId()),
      resource.getCredentialsId(),
      resource.getName(),
      resource.getName()
    ));

    final String query = insertOrUpdateResourceStatement(tenantId);

    logInsertQuery(log, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId, vertx).execute(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Void> delete(String resourceId, UUID credentialsId, String tenantId) {
    Tuple params = createParams(resourceId, credentialsId);

    final String query = deleteResourceStatement(tenantId);

    logDeleteQuery(log, query, params);

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

    final String query = selectResourcesWithTags(tenantId, tags);

    logSelectQuery(log, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId, vertx).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapResources);
  }

  private List<DbResource> mapResources(RowSet<Row> resultSet) {
    return RowSetUtils.mapItems(resultSet, entry -> DbResource.builder()
      .id(parseResourceId(entry.getString(ID_COLUMN)))
      .credentialsId(entry.getUUID(CREDENTIALS_ID_COLUMN))
      .name(entry.getString(NAME_COLUMN))
      .tags(asList(entry.getArrayOfStrings(TAG_COLUMN)))
      .build()
    );
  }
}
