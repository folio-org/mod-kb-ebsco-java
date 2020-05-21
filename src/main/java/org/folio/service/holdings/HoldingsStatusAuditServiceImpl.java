package org.folio.service.holdings;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.folio.repository.holdings.status.audit.HoldingsStatusAuditRepository;

@Component
public class HoldingsStatusAuditServiceImpl implements HoldingsStatusAuditService {
  @Autowired
  private HoldingsStatusAuditRepository repository;
  /**
   * Duration in milliseconds before records can be cleared from database
   */
  @Value("${holdings.status.audit.expiration.period}")
  private long auditExpirationPeriod;

  @Override
  public CompletableFuture<Void> clearExpiredRecords(String credentialsId, String tenantId) {
    Instant expirationLimit = Instant.now().minus(auditExpirationPeriod, ChronoUnit.MILLIS);
    return repository.deleteBeforeTimestamp(expirationLimit, credentialsId, tenantId);
  }
}
