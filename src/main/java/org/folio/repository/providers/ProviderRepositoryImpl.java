package org.folio.repository.providers;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.createPlaceholders;
import static org.folio.db.DbUtils.createParams;
import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.repository.DbUtil.DELETE_LOG_MESSAGE;
import static org.folio.repository.DbUtil.INSERT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.SELECT_LOG_MESSAGE;
import static org.folio.repository.DbUtil.getProviderTableName;
import static org.folio.repository.DbUtil.getTagsTableName;
import static org.folio.repository.DbUtil.prepareQuery;
import static org.folio.repository.providers.ProviderTableConstants.DELETE_PROVIDER_STATEMENT;
import static org.folio.repository.providers.ProviderTableConstants.ID_COLUMN;
import static org.folio.repository.providers.ProviderTableConstants.INSERT_OR_UPDATE_PROVIDER_STATEMENT;
import static org.folio.repository.providers.ProviderTableConstants.SELECT_TAGGED_PROVIDERS;
import static org.folio.util.FutureUtils.mapResult;

import java.util.Collections;
import java.util.List;
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

import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.rest.persist.PostgresClient;

@Component
public class ProviderRepositoryImpl implements ProviderRepository {

  private static final Logger LOG = LoggerFactory.getLogger(ProviderRepositoryImpl.class);

  @Autowired
  private Vertx vertx;
  @Autowired
  private DBExceptionTranslator excTranslator;

  @Override
  public CompletableFuture<Void> save(DbProvider provider, String tenantId) {
    Tuple parameters = createParams(asList(
      provider.getId(),
      provider.getCredentialsId(),
      provider.getName(),
      provider.getName())
    );

    final String query = prepareQuery(INSERT_OR_UPDATE_PROVIDER_STATEMENT, getProviderTableName(tenantId));

    LOG.info(INSERT_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Void> delete(String vendorId, String credentialsId, String tenantId) {
    Tuple parameter = createParams(asList(vendorId, credentialsId));

    final String query = prepareQuery(DELETE_PROVIDER_STATEMENT, getProviderTableName(tenantId));

    LOG.info(DELETE_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameter, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<List<Long>> findIdsByTagName(List<String> tags, int page, int count, String credentialsId,
                                                        String tenantId) {
    if (CollectionUtils.isEmpty(tags)) {
      return completedFuture(Collections.emptyList());
    }

    int offset = (page - 1) * count;

    Tuple parameters = Tuple.tuple();
    tags.forEach(parameters::addString);
    parameters
      .addString(credentialsId)
      .addInteger(offset)
      .addInteger(count);

    final String query = prepareQuery(SELECT_TAGGED_PROVIDERS, getProviderTableName(tenantId),
      getTagsTableName(tenantId), createPlaceholders(tags.size()));

    LOG.info(SELECT_LOG_MESSAGE, query);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).select(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), this::mapProviderIds);
  }

  private List<Long> mapProviderIds(RowSet<Row> resultSet) {
    return mapItems(resultSet, this::readProviderId);
  }

  private Long readProviderId(Row row) {
    return Long.parseLong(row.getString(ID_COLUMN));
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
