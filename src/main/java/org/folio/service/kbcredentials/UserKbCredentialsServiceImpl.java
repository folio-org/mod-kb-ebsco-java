package org.folio.service.kbcredentials;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;

import static org.folio.rest.tools.utils.TenantTool.tenantId;
import static org.folio.util.TokenUtils.fetchUserInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.NotFoundException;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.core.convert.converter.Converter;

import org.folio.repository.kbcredentials.DbKbCredentials;
import org.folio.repository.kbcredentials.KbCredentialsRepository;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.util.UserInfo;

public class UserKbCredentialsServiceImpl implements UserKbCredentialsService {

  private static final String USER_CREDS_NOT_FOUND_MESSAGE = "User credentials not found: userId = %s";

  private KbCredentialsRepository repository;
  private Converter<DbKbCredentials, KbCredentials> credentialsFromDBConverter;


  public UserKbCredentialsServiceImpl(KbCredentialsRepository repository,
                                      Converter<DbKbCredentials, KbCredentials> credentialsFromDBConverter) {
    this.repository = repository;
    this.credentialsFromDBConverter = credentialsFromDBConverter;
  }

  @Override
  public CompletableFuture<KbCredentials> findByUser(Map<String, String> okapiHeaders) {
    return fetchUserInfo(okapiHeaders)
      .thenCompose(userInfo -> findUserCredentials(userInfo, tenantId(okapiHeaders)))
      .thenApply(credentialsFromDBConverter::convert);
  }

  private CompletionStage<DbKbCredentials> findUserCredentials(UserInfo userInfo, String tenant) {
    return repository.findByUserId(userInfo.getUserId(), tenant)
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
    CompletableFuture<Collection<DbKbCredentials>> allCreds = repository.findAll(tenant);

    return allCreds.thenApply(credentials ->
      credentials.size() == 1
        ? Optional.of(CollectionUtils.extractSingleton(credentials))
        : Optional.empty()
    );
  }

  private Function<Optional<DbKbCredentials>, DbKbCredentials> getCredentialsOrFailWithUserId(String userId) {
    return credentials -> credentials.orElseThrow(
      () -> new NotFoundException(format(USER_CREDS_NOT_FOUND_MESSAGE, userId)));
  }

}
