package org.folio.service.kbcredentials;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import static org.folio.rest.util.RequestHeadersUtil.tenantId;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.repository.assigneduser.AssignedUserRepository;
import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.repository.kbcredentials.KbCredentialsRepository;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.util.RequestHeadersUtil;
import org.springframework.core.convert.converter.Converter;

@Log4j2
@RequiredArgsConstructor
public class UserKbCredentialsServiceImpl implements UserKbCredentialsService {

  private static final String USER_CREDS_NOT_FOUND_MESSAGE = "KB Credentials do not exist or user with userId = %s "
    + "is not assigned to any available knowledgebase.";

  private final KbCredentialsRepository credentialsRepository;
  private final AssignedUserRepository assignedUserRepository;
  private final Converter<DbKbCredentials, KbCredentials> credentialsFromDbConverter;

  private static <T> Function<Optional<T>, CompletableFuture<Optional<T>>> ifEmpty(
    Supplier<CompletableFuture<Optional<T>>> supplier) {
    return optional -> optional
      .map(value -> completedFuture(Optional.of(value)))
      .orElse(supplier.get());
  }

  @Override
  public CompletableFuture<KbCredentials> findByUser(Map<String, String> okapiHeaders) {
    return RequestHeadersUtil.userIdFuture(okapiHeaders)
      .thenCompose(userId -> findUserCredentials(userId, tenantId(okapiHeaders)))
      .thenApply(credentialsFromDbConverter::convert);
  }

  private CompletableFuture<DbKbCredentials> findUserCredentials(String userIdValue, String tenant) {
    var userId = UUID.fromString(userIdValue);
    log.debug("findUserCredentials:: Attempts to find KbCredentials by [userId: {}, tenant: {}]", userId, tenant);

    return credentialsRepository.findByUserId(userId, tenant)
      .thenCompose(ifEmpty(() -> findSingleKbCredentials(tenant)))
      .thenApply(getCredentialsOrFailWithUserId(userIdValue));
  }

  private CompletableFuture<Optional<DbKbCredentials>> findSingleKbCredentials(String tenant) {
    log.debug("findSingleKbCredentials:: Attempts to find all credentials by [tenant: {}]", tenant);
    CompletableFuture<Collection<DbKbCredentials>> allCreds = credentialsRepository.findAll(tenant);

    return allCreds.thenCompose(credentials -> {
      if (credentials.size() != 1) {
        return completedFuture(Optional.empty());
      }

      DbKbCredentials single = CollectionUtils.extractSingleton(credentials);

      return assignedUserRepository.count(single.getId(), tenant)
        .thenApply(count -> INTEGER_ZERO.equals(count)
          ? Optional.of(single)
          : Optional.empty());
    });
  }

  private Function<Optional<DbKbCredentials>, DbKbCredentials> getCredentialsOrFailWithUserId(String userId) {
    return credentials -> credentials.orElseThrow(
      () -> new NotFoundException(format(USER_CREDS_NOT_FOUND_MESSAGE, userId)));
  }

}
