package org.folio.repository.holdings;

import org.folio.service.holdings.HoldingsStatus;
import org.folio.service.holdings.message.ConfigurationMessage;
import org.folio.service.holdings.message.LoadFailedMessage;
import org.folio.service.holdings.message.LoadHoldingsMessage;
import org.folio.service.holdings.message.SnapshotCreatedMessage;
import org.folio.service.holdings.message.SnapshotFailedMessage;
import org.springframework.lang.NonNull;

public final class HoldingsServiceMessagesFactory {

  private HoldingsServiceMessagesFactory() {
  }

  @NonNull
  public static SnapshotFailedMessage getSnapshotFailedMessage(ConfigurationMessage message, Throwable throwable) {
    return new SnapshotFailedMessage(
      message.getConfiguration(),
      throwable.getMessage(),
      message.getCredentialsId(),
      message.getTenantId());
  }

  @NonNull
  public static SnapshotCreatedMessage getSnapshotCreatedMessage(ConfigurationMessage message, HoldingsStatus status,
                                                                 int requestCount) {
    return new SnapshotCreatedMessage(
      message.getConfiguration(),
      status.getTransactionId(),
      status.getTotalCount(),
      requestCount,
      message.getCredentialsId(),
      message.getTenantId());
  }

  @NonNull
  public static LoadFailedMessage getLoadFailedMessage(LoadHoldingsMessage message, Throwable throwable) {
    return new LoadFailedMessage(
      message.getConfiguration(),
      throwable.getMessage(),
      message.getCredentialsId(),
      message.getTenantId(),
      message.getCurrentTransactionId(),
      message.getTotalCount(),
      message.getTotalPages());
  }

  @NonNull
  public static LoadHoldingsMessage getLoadHoldingsMessage(SnapshotCreatedMessage message,
                                                           String previousTransactionId) {
    return new LoadHoldingsMessage(
      message.getConfiguration(),
      message.getCredentialsId(),
      message.getTenantId(),
      message.getTotalCount(),
      message.getTotalPages(),
      message.getTransactionId(),
      previousTransactionId);
  }

  @NonNull
  public static LoadHoldingsMessage getLoadHoldingsMessage(LoadFailedMessage message, String previousTransactionId) {
    return new LoadHoldingsMessage(
      message.getConfiguration(),
      message.getCredentialsId(),
      message.getTenantId(),
      message.getTotalCount(),
      message.getTotalPages(),
      message.getTransactionId(),
      previousTransactionId);
  }
}
