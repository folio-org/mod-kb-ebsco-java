package org.folio.service.holdings;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.HoldingsLoadStatus;
import org.folio.holdingsiq.service.LoadService;
import org.folio.repository.holdings.LoadStatus;
import org.folio.service.holdings.message.HoldingsMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;

@Component("DefaultLoadServiceFacade")
public class DefaultLoadServiceFacade extends AbstractLoadServiceFacade {
  @Autowired
  public DefaultLoadServiceFacade(@Value("${holdings.status.check.delay}") long statusRetryDelay,
                                  @Value("${holdings.status.retry.count}") int statusRetryCount,
                                  @Value("${holdings.page.retry.delay}") int loadPageRetryDelay,
                                  @Value("${holdings.snapshot.refresh.period}") int snapshotRefreshPeriod,
                                  @Value("${holdings.page.retry.count}") int loadPageRetryCount,
                                  Vertx vertx) {
    super(statusRetryDelay, statusRetryCount, loadPageRetryDelay, snapshotRefreshPeriod, loadPageRetryCount, vertx);
  }

  @Override
  protected CompletableFuture<String> populateHoldings(LoadService loadingService) {
    return loadingService.populateHoldings().thenApply(o -> null);
  }

  @Override
  protected CompletableFuture<HoldingsStatus> getLoadingStatus(LoadService loadingService, String transactionId) {
    return getLastLoadingStatus(loadingService);
  }

  @Override
  protected CompletableFuture<HoldingsStatus> getLastLoadingStatus(LoadService loadingService) {
    return loadingService.getLoadingStatus()
      .thenApply(this::mapToStatus);
  }

  @Override
  protected CompletableFuture<Void> loadHoldings(LoadHoldingsMessage message, LoadService loadingService) {
    return loadWithPagination(message.getTotalPages(), page ->
      loadingService.loadHoldings(getMaxPageSize(), page)
        .thenAccept(holdings -> holdingsService.saveHolding(new HoldingsMessage(holdings.getHoldingsList(), message.getTenantId(), null, message.getCredentialsId()))));
  }

  @Override
  protected int getMaxPageSize() {
    return MAX_COUNT;
  }

  protected HoldingsStatus mapToStatus(HoldingsLoadStatus status) {
    return HoldingsStatus.builder()
      .created(status.getCreated())
      .status(LoadStatus.fromValue(status.getStatus()))
      .totalCount(status.getTotalCount())
      .build();
  }
}
