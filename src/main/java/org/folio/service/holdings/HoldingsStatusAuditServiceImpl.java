package org.folio.service.holdings;

import static org.folio.db.RowSetUtils.toUUID;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import org.folio.repository.holdings.status.audit.HoldingsStatusAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HoldingsStatusAuditServiceImpl implements HoldingsStatusAuditService {

  @Autowired
  private HoldingsStatusAuditRepository repository;
  /**
   * Duration in milliseconds before records can be cleared from database.
   */
  @Value("${holdings.status.audit.expiration.period}")
  private long auditExpirationPeriod;

  @Override
  public CompletableFuture<Void> clearExpiredRecords(String credentialsId, String tenantId) {
    OffsetDateTime expirationLimit = OffsetDateTime.now().minus(auditExpirationPeriod, ChronoUnit.MILLIS);
    return repository.deleteBeforeTimestamp(expirationLimit, toUUID(credentialsId), tenantId);
  }
}
