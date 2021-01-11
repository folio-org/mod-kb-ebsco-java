package org.folio.service.kbcredentials;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;

import static org.folio.rest.tools.utils.TenantTool.tenantId;
import static org.folio.util.TokenUtils.fetchUserInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.NotFoundException;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.core.convert.converter.Converter;

import org.folio.repository.assigneduser.AssignedUserRepository;
import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.repository.kbcredentials.KbCredentialsRepository;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.util.UserInfo;

public class UserKbCredentialsServiceImpl implements UserKbCredentialsService {

  private static final String USER_CREDS_NOT_FOUND_MESSAGE = "KB Credentials do not exist or user with userId = %s " +
    "is not assigned to any available knowledgebase.";

  private final KbCredentialsRepository credentialsRepository;
  private final AssignedUserRepository assignedUserRepository;
  private final Converter<DbKbCredentials, KbCredentials> credentialsFromDBConverter;


  public UserKbCredentialsServiceImpl(KbCredentialsRepository credentialsRepository,
                                      AssignedUserRepository assignedUserRepository,
                                      Converter<DbKbCredentials, KbCredentials> credentialsFromDBConverter) {
    this.credentialsRepository = credentialsRepository;
    this.assignedUserRepository = assignedUserRepository;
    this.credentialsFromDBConverter = credentialsFromDBConverter;
  }

  @Override
  public CompletableFuture<KbCredentials> findByUser(Map<String, String> okapiHeaders) {
    return fetchUserInfo(okapiHeaders)
      .thenCompose(userInfo -> findUserCredentials(userInfo, tenantId(okapiHeaders)))
      .thenApply(credentialsFromDBConverter::convert);
  }

  private CompletionStage<DbKbCredentials> findUserCredentials(UserInfo userInfo, String tenant) {
    return credentialsRepository.findByUserId(UUID.fromString(userInfo.getUserId()), tenant)
      .thenCompose(ifEmpty(() -> findSingleKbCredentials(tenant)))
      .thenApply(getCredentialsOrFailWithUserId(userInfo.getUserId()));
  }

  private static <T> Function<Optional<T>, CompletableFuture<Optional<T>>> ifEmpty(
    Supplier<CompletableFuture<Optional<T>>> supplier) {
    return optional -> optional
      .map(value -> completedFuture(Optional.of(value)))
      .orElse(supplier.get());
  }

  private CompletableFuture<Optional<DbKbCredentials>> findSingleKbCredentials(String tenant) {
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
