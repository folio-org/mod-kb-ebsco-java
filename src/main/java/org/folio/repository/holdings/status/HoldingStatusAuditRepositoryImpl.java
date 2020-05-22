package org.folio.repository.holdings.status;

import static org.folio.common.FunctionUtils.nothing;
import static org.folio.repository.DbUtil.getHoldingsStatusAuditTableName;
import static org.folio.repository.holdings.status.HoldingsStatusAuditTableConstants.DELETE_BEFORE_TIMESTAMP;
import static org.folio.util.FutureUtils.mapVertxFuture;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.rest.persist.PostgresClient;

@Component
public class HoldingStatusAuditRepositoryImpl implements HoldingsStatusAuditRepository{

  private static final Logger LOG = LoggerFactory.getLogger(HoldingStatusAuditRepositoryImpl.class);
  private Vertx vertx;

  @Autowired
  public HoldingStatusAuditRepositoryImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  public CompletableFuture<Void> deleteBeforeTimestamp(Instant timestamp, String tenantId){
    final String query = String.format(DELETE_BEFORE_TIMESTAMP, getHoldingsStatusAuditTableName(tenantId), timestamp.toString());
    PostgresClient postgresClient = PostgresClient.getInstance(vertx, tenantId);
    LOG.info("Delete holdings status audit records before timestamp = " + query);
    Promise<UpdateResult> promise = Promise.promise();
    postgresClient.execute(query, promise);
    return mapVertxFuture(promise.future())
      .thenApply(nothing());
  }
}
