package org.folio.service.holdings;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import org.folio.repository.holdings.DbHoldingInfo;
import org.folio.rest.util.template.RMAPITemplateContext;
import org.folio.service.holdings.message.DeltaReportCreatedMessage;
import org.folio.service.holdings.message.DeltaReportMessage;
import org.folio.service.holdings.message.HoldingsMessage;
import org.folio.service.holdings.message.LoadFailedMessage;
import org.folio.service.holdings.message.SnapshotCreatedMessage;
import org.folio.service.holdings.message.SnapshotFailedMessage;

@ProxyGen
@VertxGen
public interface HoldingsService {

  @GenIgnore
  static HoldingsService createProxy(Vertx vertx, String address) {
    return new HoldingsServiceVertxEBProxy(vertx, address);
  }

  /**
   * Starts the process of loading holdings
   * @param context RMAPITemplateContext
   * @return future that will be completed when process is started successfully, if process failed to start then future
   * will be failed.
   */
  @GenIgnore
  default CompletableFuture<Void> loadSingleHoldings(RMAPITemplateContext context) {
    //Default implementation is necessary for automatically generated vertx proxy
    throw new UnsupportedOperationException();
  }

  @GenIgnore
  default CompletableFuture<List<DbHoldingInfo>> getHoldingsByIds(List<String> ids, String credentialsId, String tenant) {
    throw new UnsupportedOperationException();
  }

  @GenIgnore
  default CompletableFuture<Boolean> canStartLoading(String tenant) {
    throw new UnsupportedOperationException();
  }

  @GenIgnore
  default CompletableFuture<Boolean> canStartLoading(String credentialsId, String tenant) {
    throw new UnsupportedOperationException();
  }

  @GenIgnore
  default CompletableFuture<List<DbHoldingInfo>> getHoldingsByPackageId(String packageId, String credentialsId, String tenant) {
    throw new UnsupportedOperationException();
  }

  void saveHolding(HoldingsMessage holdings);

  void processChanges(DeltaReportMessage holdings);

  void snapshotCreated(SnapshotCreatedMessage message);

  void snapshotFailed(SnapshotFailedMessage message);

  void deltaReportCreated(DeltaReportCreatedMessage message, Handler<AsyncResult<Void>> handler);

  void loadingFailed(LoadFailedMessage message);
}
