package org.folio.service.holdings;

import lombok.Builder;
import lombok.Value;

import org.folio.repository.holdings.LoadStatus;

@Value
@Builder(toBuilder = true)
public class HoldingsStatus {
  String transactionId;
  LoadStatus status;
  String created;
  Integer totalCount;
  Integer totalPages;
}
