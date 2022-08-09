package org.folio.service.holdings.message;

import org.folio.holdingsiq.model.DeltaReport;
import org.folio.holdingsiq.model.Holdings;
import org.jetbrains.annotations.NotNull;

public final class MessageFactory {

  private MessageFactory() { }

  @NotNull
  public static DeltaReportMessage getDeltaReportMessage(LoadHoldingsMessage message, DeltaReport holdings) {
    return new DeltaReportMessage(
      holdings.getHoldings(),
      message.getTenantId(),
      message.getCurrentTransactionId(),
      message.getCredentialsId());
  }

  @NotNull
  public static DeltaReportCreatedMessage getDeltaReportCreatedMessage(LoadHoldingsMessage message, int totalCount,
                                                                       int requestCount) {
    return new DeltaReportCreatedMessage(
      message.getConfiguration(),
      totalCount,
      requestCount,
      message.getTenantId(),
      message.getCredentialsId());
  }

  @NotNull
  public static HoldingsMessage getHoldingsMessage(LoadHoldingsMessage message, Holdings holdings) {
    return new HoldingsMessage(
      holdings.getHoldingsList(),
      message.getTenantId(),
      message.getCurrentTransactionId(),
      message.getCredentialsId());
  }
}
