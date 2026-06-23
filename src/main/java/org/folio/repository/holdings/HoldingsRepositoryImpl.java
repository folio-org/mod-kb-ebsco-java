package org.folio.repository.holdings;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.common.ListUtils.mapItems;
import static org.folio.common.LogUtils.logDeleteQuery;
import static org.folio.common.LogUtils.logInsertQuery;
import static org.folio.repository.holdings.HoldingsTableConstants.deleteByPkHoldings;
import static org.folio.repository.holdings.HoldingsTableConstants.deleteOldRecordsByCredentialsId;
import static org.folio.repository.holdings.HoldingsTableConstants.insertOrUpdateHoldings;
import static org.folio.util.FutureUtils.mapVertxFuture;

import com.google.common.collect.Lists;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnection;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import lombok.extern.log4j.Log4j2;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.util.IdParser;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class HoldingsRepositoryImpl implements HoldingsRepository {
  private static final int MAX_BATCH_SIZE = 200;

  private final Vertx vertx;

  public HoldingsRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<Void> saveAll(Set<DbHoldingInfo> holdings, OffsetDateTime updatedAt, UUID credentialsId,
                                         String tenantId) {
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    Future<Void> future = postgresClient.withTransaction(connection ->
      executeInBatches(holdings,
        batch -> saveHoldings(batch, updatedAt, credentialsId, tenantId, connection))
    );

    return mapVertxFuture(future);
  }

  @Override
  public CompletableFuture<Void> deleteBeforeTimestamp(OffsetDateTime timestamp, UUID credentialsId, String tenantId) {
    final String query = deleteOldRecordsByCredentialsId(tenantId);
    final Tuple params = Tuple.of(credentialsId, timestamp);
    logDeleteQuery(log, query, params);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient(tenantId).execute(query, params, promise::handle);
    return mapVertxFuture(promise.future()).thenApply(nothing());
  }

  @Override
  public CompletableFuture<Void> deleteAll(Set<HoldingsId> holdings, UUID credentialsId, String tenantId) {
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);

    Future<Void> future = postgresClient.withTransaction(conn ->
      executeInBatches(holdings,
        batch -> deleteHoldings(batch, credentialsId, tenantId, conn))
    );

    return mapVertxFuture(future);
  }

  private Future<Void> saveHoldings(List<DbHoldingInfo> holdings, OffsetDateTime updatedAt,
                                    UUID credentialsId, String tenantId,
                                    PgConnection connection) {
    final Tuple parameters = createParameters(credentialsId, holdings, updatedAt);
    final String query = insertOrUpdateHoldings(tenantId, holdings);
    logInsertQuery(log, query, parameters);

    Promise<RowSet<Row>> promise = Promise.promise();
    connection
      .preparedQuery(query)
      .execute(parameters)
      .onComplete(promise);

    return promise.future().map(nothing());
  }

  private Future<Void> deleteHoldings(List<HoldingsId> holdings, UUID credentialsId, String tenantId,
                                      PgConnection connection) {
    var params = getHoldingsPkKeysParams(credentialsId, mapItems(holdings, IdParser::getResourceId));
    var query = deleteByPkHoldings(tenantId, holdings);
    logDeleteQuery(log, query, params);

    Promise<RowSet<Row>> promise = Promise.promise();
    connection
      .preparedQuery(query)
      .execute(params)
      .onComplete(promise);

    return promise.future().map(nothing());
  }

  /**
   * Splits items into batches and sequentially executes batchOperation on each batch.
   *
   * @param <T>            Type of process items
   * @param items          items to process in batches
   * @param batchOperation operation to execute on each batch
   * @return future that will be completed when all batches are successfully processed
   */
  private <T> Future<Void> executeInBatches(Set<T> items,
                                            Function<List<T>, Future<Void>> batchOperation) {
    List<List<T>> batches = Lists.partition(Lists.newArrayList(items), HoldingsRepositoryImpl.MAX_BATCH_SIZE);
    Future<Void> future = Future.succeededFuture();
    for (List<T> batch : batches) {
      future = future.compose(o -> batchOperation.apply(batch));
    }
    return future;
  }

  private String getHoldingsId(DbHoldingInfo holding) {
    return holding.getVendorId() + "-" + holding.getPackageId() + "-" + holding.getTitleId();
  }

  private Tuple createParameters(UUID credentialsId, List<DbHoldingInfo> holdings, OffsetDateTime updatedAt) {
    Tuple params = Tuple.tuple();
    holdings.forEach(holding -> {
      params.addValue(credentialsId);
      params.addValue(getHoldingsId(holding));
      params.addValue(holding.getVendorId());
      params.addValue(holding.getPackageId());
      params.addValue(holding.getTitleId());
      params.addValue(holding.getResourceType());
      params.addValue(holding.getPublisherName());
      params.addValue(holding.getPublicationTitle());
      params.addValue(updatedAt);
    });
    return params;
  }

  private Tuple getHoldingsPkKeysParams(UUID credentialsId, List<String> resourceIds) {
    var parameters = Tuple.tuple();
    for (String resourceId : resourceIds) {
      parameters.addUUID(credentialsId).addString(resourceId);
    }
    return parameters;
  }

  private PostgresClient pgClient(String tenantId) {
    return PostgresClient.getInstance(vertx, tenantId);
  }
}
