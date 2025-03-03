package org.folio.service.holdings;

import static org.folio.repository.holdings.LoadStatus.COMPLETED;
import static org.folio.repository.holdings.LoadStatus.FAILED;
import static org.folio.repository.holdings.LoadStatus.IN_PROGRESS;
import static org.folio.service.holdings.message.MessageFactory.getDeltaReportCreatedMessage;
import static org.folio.service.holdings.message.MessageFactory.getDeltaReportMessage;
import static org.folio.service.holdings.message.MessageFactory.getHoldingsMessage;
import static org.folio.util.FutureUtils.mapVertxFuture;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.folio.holdingsiq.model.DeltaReportStatus;
import org.folio.holdingsiq.model.HoldingsDownloadTransaction;
import org.folio.holdingsiq.model.HoldingsLoadTransactionStatus;
import org.folio.holdingsiq.model.HoldingsTransactionIdsList;
import org.folio.holdingsiq.model.TransactionId;
import org.folio.holdingsiq.service.LoadService;
import org.folio.repository.holdings.LoadStatus;
import org.folio.repository.holdings.ReportStatus;
import org.folio.service.holdings.message.LoadHoldingsMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Log4j2
@Component("transactionLoadServiceFacade")
public class TransactionLoadServiceFacade extends AbstractLoadServiceFacade {

  private static final int MAX_SIZE = 4000;
  private static final int DELTA_REPORT_MAX_SIZE = 4000;
  private static final Map<String, LoadStatus> TRANSACTION_STATUS_TO_LOAD_STATUS = Map.of(
    "In Progress", IN_PROGRESS,
    "Complete", COMPLETED,
    "Failed", FAILED
  );

  private final long reportStatusRetryDelay;
  private final int reportStatusRetryCount;

  public TransactionLoadServiceFacade(@Value("${holdings.status.check.delay}") long statusRetryDelay,
                                      @Value("${holdings.status.retry.count}") int statusRetryCount,
                                      @Value("${holdings.report.status.check.delay}") long reportStatusRetryDelay,
                                      @Value("${holdings.report.status.retry.count}") int reportStatusRetryCount,
                                      @Value("${holdings.page.retry.delay}") int loadPageRetryDelay,
                                      @Value("${holdings.snapshot.refresh.period}") int snapshotRefreshPeriod,
                                      @Value("${holdings.page.retry.count}") int loadPageRetryCount,
                                      @Value("${holdings.page.size.min}") int loadPageSizeMin,
                                      Vertx vertx) {
    super(statusRetryDelay, statusRetryCount, loadPageRetryDelay, loadPageRetryCount, loadPageSizeMin,
      snapshotRefreshPeriod, vertx);
    this.reportStatusRetryDelay = reportStatusRetryDelay;
    this.reportStatusRetryCount = reportStatusRetryCount;
  }

  @Override
  protected CompletableFuture<Void> loadHoldings(LoadHoldingsMessage message, LoadService loadingService) {
    log.debug("loadHoldings:: by [tenant: {}]", message.getTenantId());

    return transactionExists(message.getPreviousTransactionId(), loadingService)
      .thenCompose(previousTransactionExists -> {
        if (Boolean.FALSE.equals(previousTransactionExists)) {
          log.debug("Previous transaction does not exist, attempts to load current transaction & save");
          return loadWithPagination(message.getTotalPages(),
            page -> loadingService.loadHoldingsTransaction(message.getCurrentTransactionId(), getMaxPageSize(), page)
              .thenAccept(holdings -> holdingsService.saveHolding(getHoldingsMessage(message, holdings))));
        } else {
          MutableObject<String> deltaReportId = new MutableObject<>();
          MutableObject<DeltaReportStatus> deltaReportStatus = new MutableObject<>();
          return loadingService.populateDeltaReport(message.getCurrentTransactionId(),
              message.getPreviousTransactionId())
            .thenCompose(id -> {
              deltaReportId.setValue(id);
              return waitForReportToComplete(loadingService, id);
            })
            .thenCompose(status -> {
              deltaReportStatus.setValue(status);
              return sendDeltaReportCreatedMessage(message, status);
            })
            .thenCompose(o -> {
              int totalPages =
                getRequestCount(Integer.valueOf(deltaReportStatus.getValue().getTotalCount()), DELTA_REPORT_MAX_SIZE);
              return loadWithPagination(totalPages,
                page -> loadingService.loadDeltaReport(deltaReportId.getValue(), DELTA_REPORT_MAX_SIZE, page)
                  .thenAccept(holdings -> holdingsService.processChanges(getDeltaReportMessage(message, holdings))));
            });
        }
      });
  }

  @Override
  protected CompletableFuture<String> populateHoldings(LoadService loadingService) {
    return loadingService.populateHoldingsTransaction()
      .thenApply(TransactionId::getTransactionId);
  }

  @Override
  protected CompletableFuture<HoldingsStatus> getLastLoadingStatus(LoadService loadingService) {
    return loadingService.getTransactions().thenCompose(transactions -> {
      if (transactions.getHoldingsDownloadTransactionIds().isEmpty()) {
        return CompletableFuture.completedFuture(createNoneStatus());
      }
      List<HoldingsDownloadTransaction> sortedTransactions = sortByDate(transactions);
      HoldingsDownloadTransaction lastTransaction = sortedTransactions.getLast();
      return getLoadingStatus(loadingService, lastTransaction.getTransactionId());
    });
  }

  @Override
  protected CompletableFuture<HoldingsStatus> getLoadingStatus(LoadService loadingService, String transactionId) {
    return loadingService.getTransactionStatus(transactionId)
      .thenApply(transaction -> mapCompletedTransactionStatus(transaction, transactionId));
  }

  @Override
  protected int getMaxPageSize() {
    return MAX_SIZE;
  }

  private CompletableFuture<Boolean> transactionExists(String transactionId, LoadService loadingService) {
    if (transactionId == null) {
      return CompletableFuture.completedFuture(false);
    }
    return loadingService.getTransactions()
      .thenApply(transactions -> transactions.getHoldingsDownloadTransactionIds().stream()
        .anyMatch(transaction -> transaction.getTransactionId().equals(transactionId)));
  }

  @NonNull
  private CompletionStage<Void> sendDeltaReportCreatedMessage(LoadHoldingsMessage message, DeltaReportStatus status) {
    Promise<Void> promise = Promise.promise();
    int totalCount = Integer.parseInt(status.getTotalCount());
    holdingsService.deltaReportCreated(
      getDeltaReportCreatedMessage(message, totalCount, getRequestCount(totalCount, DELTA_REPORT_MAX_SIZE)), promise);
    return mapVertxFuture(promise.future());
  }

  private CompletableFuture<DeltaReportStatus> waitForReportToComplete(LoadService loadingService,
                                                                       String deltaReportId) {
    return doUntilResultMatches(reportStatusRetryCount, reportStatusRetryDelay,
      retries -> loadingService.getDeltaReportStatus(deltaReportId),
      (loadStatus, ex) -> ReportStatus.COMPLETED == ReportStatus.fromValue(loadStatus.getStatus())
    );
  }

  private HoldingsStatus mapCompletedTransactionStatus(HoldingsLoadTransactionStatus transaction,
                                                       String transactionId) {
    return HoldingsStatus.builder()
      .created(transaction.getCreationDate())
      .status(TRANSACTION_STATUS_TO_LOAD_STATUS.get(transaction.getStatus()))
      .totalCount(
        StringUtils.isEmpty(transaction.getTotalCount()) ? null : Integer.parseInt(transaction.getTotalCount()))
      .transactionId(transactionId)
      .build();
  }

  private HoldingsStatus createNoneStatus() {
    return HoldingsStatus.builder()
      .status(LoadStatus.NONE)
      .build();
  }

  private List<HoldingsDownloadTransaction> sortByDate(HoldingsTransactionIdsList transactions) {
    return transactions.getHoldingsDownloadTransactionIds().stream()
      .sorted(Comparator.comparing(
        transaction -> LocalDateTime.parse(transaction.getCreationDate(), HOLDINGS_STATUS_TIME_FORMATTER)
          .atZone(ZoneOffset.UTC)))
      .toList();
  }
}
