package org.folio.service.holdings;

import lombok.Builder;
import lombok.Value;

import org.folio.repository.holdings.LoadStatus;

@Value
@Builder(toBuilder = true)
public class HoldingsStatus {
  private String transactionId;
  private LoadStatus status;
  private String created;
  private Integer totalCount;
  private Integer totalPages;
}
