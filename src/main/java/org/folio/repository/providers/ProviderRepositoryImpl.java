package org.folio.repository.providers;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.LogUtils.logDeleteQueryInfoLevel;
import static org.folio.common.LogUtils.logInsertQueryInfoLevel;
import static org.folio.common.LogUtils.logSelectQueryInfoLevel;
import static org.folio.db.DbUtils.createParams;
import static org.folio.db.RowSetUtils.mapItems;
import static org.folio.repository.providers.ProviderTableConstants.ID_COLUMN;
import static org.folio.repository.providers.ProviderTableConstants.deleteProviderStatement;
import static org.folio.repository.providers.ProviderTableConstants.insertOrUpdateProviderStatement;
import static org.folio.repository.providers.ProviderTableConstants.selectTaggedProviders;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.db.exc.translation.DBExceptionTranslator;
import org.folio.rest.model.filter.TagFilter;
import org.folio.rest.persist.PostgresClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProviderRepositoryImpl implements ProviderRepository {

  private static final Logger LOG = LogManager.getLogger(ProviderRepositoryImpl.class);

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

    final String query = insertOrUpdateProviderStatement(tenantId);

    logInsertQueryInfoLevel(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<Void> delete(String vendorId, UUID credentialsId, String tenantId) {
    Tuple parameters = createParams(vendorId, credentialsId);

    final String query = deleteProviderStatement(tenantId);

    logDeleteQueryInfoLevel(LOG, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, parameters, promise);

    return mapResult(promise.future().recover(excTranslator.translateOrPassBy()), nothing());
  }

  @Override
  public CompletableFuture<List<Long>> findIdsByTagFilter(TagFilter tagFilter, UUID credentialsId, String tenantId) {
    List<String> tags = tagFilter.getTags();
    if (CollectionUtils.isEmpty(tags)) {
      return completedFuture(Collections.emptyList());
    }

    Tuple parameters = Tuple.tuple();
    tags.forEach(parameters::addString);
    parameters
      .addUUID(credentialsId)
      .addInteger(tagFilter.getOffset())
      .addInteger(tagFilter.getCount());

    final String query = selectTaggedProviders(tenantId, tags);

    logSelectQueryInfoLevel(LOG, query, parameters);

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
