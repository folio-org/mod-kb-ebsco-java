package org.folio.service.holdings;

import io.vertx.core.Vertx;
import java.util.concurrent.CompletableFuture;
import org.folio.holdingsiq.model.HoldingsLoadStatus;
import org.folio.holdingsiq.service.LoadService;
import org.folio.repository.holdings.LoadStatus;
import org.folio.service.holdings.message.HoldingsMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("DefaultLoadServiceFacade")
public class DefaultLoadServiceFacade extends AbstractLoadServiceFacade {

  private final int loadPageSize;

  @Autowired
  public DefaultLoadServiceFacade(@Value("${holdings.status.check.delay}") long statusRetryDelay,
                                  @Value("${holdings.status.retry.count}") int statusRetryCount,
                                  @Value("${holdings.page.retry.delay}") int loadPageRetryDelay,
                                  @Value("${holdings.snapshot.refresh.period}") int snapshotRefreshPeriod,
                                  @Value("${holdings.page.retry.count}") int loadPageRetryCount,
                                  @Value("${holdings.page.size:2500}") int loadPageSize,
                                  Vertx vertx) {
    super(statusRetryDelay, statusRetryCount, loadPageRetryDelay, snapshotRefreshPeriod, loadPageRetryCount, vertx);
    this.loadPageSize = loadPageSize;
  }

  @Override
  protected CompletableFuture<Void> loadHoldings(LoadHoldingsMessage message, LoadService loadingService) {
    return
      loadWithPagination(message.getTotalPages(), page -> loadingService.loadHoldings(getMaxPageSize(), page)
        .thenAccept(holdings -> holdingsService.saveHolding(
          new HoldingsMessage(holdings.getHoldingsList(), message.getTenantId(), null, message.getCredentialsId()))));
  }

  @Override
  protected CompletableFuture<String> populateHoldings(LoadService loadingService) {
    return loadingService.populateHoldings().thenApply(o -> null);
  }

  @Override
  protected CompletableFuture<HoldingsStatus> getLastLoadingStatus(LoadService loadingService) {
    return loadingService.getLoadingStatus()
      .thenApply(this::mapToStatus);
  }

  @Override
  protected CompletableFuture<HoldingsStatus> getLoadingStatus(LoadService loadingService, String transactionId) {
    return getLastLoadingStatus(loadingService);
  }

  @Override
  protected int getMaxPageSize() {
    return loadPageSize;
  }

  private HoldingsStatus mapToStatus(HoldingsLoadStatus status) {
    if (status == null) {
      return createNoneStatus();
    }

    return HoldingsStatus.builder()
      .created(status.getCreated())
      .status(LoadStatus.fromValue(status.getStatus()))
      .totalCount(status.getTotalCount())
      .build();
  }

  private HoldingsStatus createNoneStatus() {
    return HoldingsStatus.builder()
      .status(LoadStatus.NONE)
      .build();
  }
}
