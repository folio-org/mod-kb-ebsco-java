package org.folio.service.holdings;

import io.vertx.core.Vertx;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;
import lombok.extern.log4j.Log4j2;
import org.folio.holdingsiq.model.HoldingsLoadStatus;
import org.folio.holdingsiq.service.LoadService;
import org.folio.repository.holdings.LoadStatus;
import org.folio.service.holdings.message.HoldingsMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("defaultLoadServiceFacade")
@Log4j2
public class DefaultLoadServiceFacade extends AbstractLoadServiceFacade {

  private final int loadPageSize;

  @Autowired
  public DefaultLoadServiceFacade(@Value("${holdings.status.check.delay}") long statusRetryDelay,
                                  @Value("${holdings.status.retry.count}") int statusRetryCount,
                                  @Value("${holdings.page.retry.delay}") int loadPageRetryDelay,
                                  @Value("${holdings.snapshot.refresh.period}") int snapshotRefreshPeriod,
                                  @Value("${holdings.page.retry.count}") int loadPageRetryCount,
                                  @Value("${holdings.page.size:2500}") int loadPageSize,
                                  @Value("${holdings.page.size.min}") int loadPageSizeMin,
                                  Vertx vertx) {
    super(statusRetryDelay, statusRetryCount, loadPageRetryDelay, loadPageRetryCount, loadPageSizeMin,
      snapshotRefreshPeriod, vertx);
    this.loadPageSize = loadPageSize;
  }

  @Override
  protected CompletableFuture<Void> loadHoldings(LoadHoldingsMessage message, LoadService loadingService) {
    return loadWithPagination(message.getTotalPages(), offset -> loadingService.loadHoldings(getMaxPageSize(), offset)
      .thenAccept(holdings -> holdingsService.saveHolding(
        new HoldingsMessage(holdings.getHoldingsList(), message.getTenantId(), null, message.getCredentialsId()))));
  }

  @Override
  protected CompletableFuture<String> populateHoldings(LoadService loadingService) {
    return loadingService.populateHoldingsForce().thenApply(o -> null);
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

  /**
   * Convert the page index (1..totalPages) into a record offset based on the page size.
   * page 1 -> offset 1     (records 1..2500)
   * page 2 -> offset 2501  (records 2501..5000)
   *
   * @return CompletableFuture that will be completed when offset is loaded
   */
  @Override
  protected CompletableFuture<Void> calculateOffset(IntFunction<CompletableFuture<Void>> offsetLoader, Integer page,
                                                    Integer retries) {
    int offset = (page - 1) * getMaxPageSize() + 1;
    log.info("Calculated offset {} for page {}, retry {}", offset, page, retries);
    return offsetLoader.apply(offset);
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
