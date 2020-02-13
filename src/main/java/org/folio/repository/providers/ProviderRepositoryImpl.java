package org.folio.repository.providers;

import static org.folio.common.FutureUtils.mapResult;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.repository.DbUtil.createInsertOrUpdateParameters;
import static org.folio.repository.DbUtil.getProviderTableName;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.providers.ProviderTableConstants.DELETE_PROVIDER_STATEMENT;
import static org.folio.repository.providers.ProviderTableConstants.INSERT_OR_UPDATE_PROVIDER_STATEMENT;
import static org.folio.repository.providers.ProviderTableConstants.SELECT_TAGGED_PROVIDERS;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

import org.folio.rest.persist.PostgresClient;

@Component
public class ProviderRepositoryImpl implements ProviderRepository {

  private static final Logger LOG = LoggerFactory.getLogger(ProviderRepositoryImpl.class);

  private Vertx vertx;

  @Autowired
  public ProviderRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<Void> save(ProviderInfoInDb provider, String tenantId) {
    JsonArray parameters = createInsertOrUpdateParameters(provider.getId(), provider.getName());

    final String query = String.format(INSERT_OR_UPDATE_PROVIDER_STATEMENT, getProviderTableName(tenantId));
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do insert query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    postgresClient.execute(query, parameters, promise);
    return mapVertxFuture(promise.future()).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<Void> delete(String vendorId, String tenantId) {
    JsonArray parameter = new JsonArray(Collections.singletonList(vendorId));

    final String query = String.format(DELETE_PROVIDER_STATEMENT, getProviderTableName(tenantId));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Do delete query = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    postgresClient.execute(query, parameter, promise);
    return mapVertxFuture(promise.future()).thenApply(result -> null);
  }

  @Override
  public CompletableFuture<List<Long>> findIdsByTagName(List<String> tags, int page, int count, String tenantId) {
    if(CollectionUtils.isEmpty(tags)){
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
    int offset = (page - 1) * count;

    JsonArray parameters = new JsonArray();
    tags.forEach(parameters::add);
    parameters
      .add(offset)
      .add(count);

    final String query = String.format(SELECT_TAGGED_PROVIDERS, getProviderTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(tags.size()));

    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    LOG.info("Select providers by tags = " + query);
    Promise<ResultSet> promise = Promise.promise();
    postgresClient.select(query, parameters, promise);

    return mapResult(promise.future(), this::mapProviderIds);
  }

  private List<Long> mapProviderIds(ResultSet resultSet) {
    return mapItems(resultSet.getRows(), this::readProviderId);
  }

  private Long readProviderId(JsonObject row) {
    return Long.parseLong(row.getString("id"));
  }
}
