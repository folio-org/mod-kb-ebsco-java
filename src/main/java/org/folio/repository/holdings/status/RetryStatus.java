package org.folio.repository.holdings.status;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class RetryStatus {
  private int retryAttemptsLeft;
  private Long timerId;
}
