package org.folio.service.holdings;

import static org.folio.repository.holdings.LoadStatus.COMPLETED;
import static org.folio.repository.holdings.LoadStatus.FAILED;
import static org.folio.repository.holdings.LoadStatus.IN_PROGRESS;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.common.FutureUtils;
import org.folio.holdingsiq.model.DeltaReportStatus;
import org.folio.holdingsiq.model.HoldingsDownloadTransaction;
import org.folio.holdingsiq.model.HoldingsLoadTransactionStatus;
import org.folio.holdingsiq.model.HoldingsTransactionIdsList;
import org.folio.holdingsiq.model.TransactionId;
import org.folio.holdingsiq.service.LoadService;
import org.folio.repository.holdings.LoadStatus;
import org.folio.repository.holdings.ReportStatus;
import org.folio.service.holdings.message.DeltaReportCreatedMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;

@Component("TransactionLoadServiceFacade")
public class TransactionLoadServiceFacade extends AbstractLoadServiceFacade {

  private static final int MAX_SIZE = 4000;
  private static final int DELTA_REPORT_MAX_SIZE = 4000;
  private static Map<String, LoadStatus> transactionStatusToLoadStatus = ImmutableMap.of(
    "In Progress", IN_PROGRESS,
    "Complete", COMPLETED,
    "Failed", FAILED
  );

  private long reportStatusRetryDelay;
  private int reportStatusRetryCount;

  public TransactionLoadServiceFacade(
                                      @Value("${holdings.status.check.delay}") long statusRetryDelay,
                                      @Value("${holdings.status.retry.count}") int statusRetryCount,
                                      @Value("${holdings.report.status.check.delay}") long reportStatusRetryDelay,
                                      @Value("${holdings.report.status.retry.count}") int reportStatusRetryCount,
                                      @Value("${holdings.page.retry.delay}") int loadPageRetryDelay,
                                      @Value("${holdings.snapshot.refresh.period}") int snapshotRefreshPeriod,
                                      @Value("${holdings.page.retry.count}") int loadPageRetryCount,

                                      Vertx vertx) {
    super(statusRetryDelay, statusRetryCount, loadPageRetryDelay, snapshotRefreshPeriod, loadPageRetryCount, vertx);
    this.reportStatusRetryDelay = reportStatusRetryDelay;
    this.reportStatusRetryCount = reportStatusRetryCount;
  }

  @Override
  protected CompletableFuture<HoldingsStatus> getLastLoadingStatus(LoadService loadingService) {
    return loadingService.getTransactions().thenCompose(transactions -> {
      if(transactions.getHoldingsDownloadTransactionIds().isEmpty()){
        return CompletableFuture.completedFuture(createNoneStatus());
      }
      List<HoldingsDownloadTransaction> sortedTransactions = sortByDate(transactions);
      HoldingsDownloadTransaction lastTransaction = sortedTransactions.get(sortedTransactions.size() - 1);
      if(!Objects.equals(transactionStatusToLoadStatus.get(lastTransaction.getStatus()), IN_PROGRESS)) {
        return getLoadingStatus(loadingService, lastTransaction.getTransactionId());
      }
      else{
        return CompletableFuture.completedFuture(mapInProgressTransaction(lastTransaction));
      }
    });
  }

  @Override
  protected CompletableFuture<String> populateHoldings(LoadService loadingService) {
    return loadingService.populateHoldingsTransaction()
      .thenApply(TransactionId::getTransactionId);
  }

  @Override
  protected CompletableFuture<HoldingsStatus> getLoadingStatus(LoadService loadingService, String transactionId) {
    return loadingService.getTransactionStatus(transactionId)
      .thenApply(transaction -> mapCompletedTransactionStatus(transaction, transactionId));
  }

  @Override
  protected CompletableFuture<Void> loadHoldings(LoadHoldingsMessage message, LoadService loadingService) {
    if(message.getPreviousTransactionId() == null){
      return loadWithPagination(message.getTotalPages(), page ->
        loadingService.loadHoldingsTransaction(message.getCurrentTransactionId(), getMaxPageSize(), page)
          .thenAccept(holdings -> holdingsService.saveHolding(new HoldingsMessage(holdings.getHoldingsList(), message.getTenantId(), message.getCurrentTransactionId()))));
    } else{
      MutableObject<String> deltaReportId = new MutableObject<>();
      MutableObject<DeltaReportStatus> deltaReportStatus = new MutableObject<>();
      return loadingService.populateDeltaReport(message.getCurrentTransactionId(), message.getPreviousTransactionId())
        .thenCompose(id -> {
          deltaReportId.setValue(id);
          return waitForReportToComplete(loadingService, id);
        })
        .thenCompose(status -> {
          deltaReportStatus.setValue(status);
          return sendDeltaReportCreatedMessage(message, status);
        })
        .thenCompose(o -> {
          int totalPages = getRequestCount(Integer.valueOf(deltaReportStatus.getValue().getTotalCount()), DELTA_REPORT_MAX_SIZE);
          return loadWithPagination(totalPages,
            page ->
              loadingService.loadDeltaReport(deltaReportId.getValue(), DELTA_REPORT_MAX_SIZE, page)
                .thenAccept(holdings -> holdingsService.processChanges(new DeltaReportMessage(holdings.getHoldings(),
                  message.getTenantId(), message.getCurrentTransactionId()))));
        });
    }
  }

  @NotNull
  private CompletionStage<Void> sendDeltaReportCreatedMessage(LoadHoldingsMessage message, DeltaReportStatus status) {
    Promise<Void> promise = Promise.promise();
    Integer totalCount = Integer.parseInt(status.getTotalCount());
    holdingsService.deltaReportCreated(new DeltaReportCreatedMessage(message.getConfiguration(),
      totalCount, getRequestCount(totalCount, DELTA_REPORT_MAX_SIZE), message.getTenantId()), promise);
    return FutureUtils.mapVertxFuture(promise.future());
  }

  @Override
  protected int getMaxPageSize() {
    return MAX_SIZE;
  }

  private CompletableFuture<DeltaReportStatus> waitForReportToComplete(LoadService loadingService, String deltaReportId) {
    return doUntilResultMatches(reportStatusRetryCount, reportStatusRetryDelay,
      () -> loadingService.getDeltaReportStatus(deltaReportId),
      (loadStatus, ex) -> ReportStatus.COMPLETED == ReportStatus.fromValue(loadStatus.getStatus())
    );
  }

  private HoldingsStatus mapCompletedTransactionStatus(HoldingsLoadTransactionStatus transaction, String transactionId) {
    return HoldingsStatus.builder()
    .created(transaction.getCreationDate())
    .status(transactionStatusToLoadStatus.get(transaction.getStatus()))
    .totalCount(StringUtils.isEmpty(transaction.getTotalCount()) ? null : Integer.parseInt(transaction.getTotalCount()))
    .transactionId(transactionId)
    .build();
  }

  private HoldingsStatus mapInProgressTransaction(HoldingsDownloadTransaction lastTransaction) {
    return HoldingsStatus.builder()
      .created(lastTransaction.getCreationDate())
      .status(transactionStatusToLoadStatus.get(lastTransaction.getStatus()))
      .transactionId(lastTransaction.getTransactionId())
      .build();
  }

  private HoldingsStatus createNoneStatus() {
    return HoldingsStatus.builder()
      .status(LoadStatus.NONE)
      .build();
  }

  private List<HoldingsDownloadTransaction> sortByDate(HoldingsTransactionIdsList transactions) {
    return transactions.getHoldingsDownloadTransactionIds().stream()
      .sorted(Comparator.comparing(transaction -> LocalDateTime.parse(transaction.getCreationDate(), HOLDINGS_STATUS_TIME_FORMATTER).atZone(ZoneOffset.UTC)))
      .collect(Collectors.toList());
  }
}
