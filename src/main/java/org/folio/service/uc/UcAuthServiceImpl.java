package org.folio.service.uc;

import static org.folio.rest.util.RequestHeadersUtil.tenantId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.folio.cache.VertxCache;
import org.folio.client.uc.UcAuthEbscoClient;
import org.folio.client.uc.model.UcAuthToken;
import org.folio.repository.uc.DbUcCredentials;
import org.folio.repository.uc.UcCredentialsRepository;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.UCCredentials;
import org.folio.rest.jaxrs.model.UCCredentialsClientId;
import org.folio.rest.jaxrs.model.UCCredentialsClientSecret;
import org.folio.rest.jaxrs.model.UCCredentialsPresence;
import org.folio.rest.jaxrs.model.UCCredentialsPresenceAttributes;
import org.springframework.stereotype.Service;

@Service
public class UcAuthServiceImpl implements UcAuthService {

  public static final String TOKEN_CACHE_KEY = "TOKEN_KEY";
  public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid UC API Credentials";
  public static final String INVALID_UC_CREDENTIALS_MESSAGE = "Invalid Usage Consolidation Credentials";

  private final UcCredentialsRepository repository;
  private final VertxCache<String, String> ucTokenCache;
  private final UcAuthEbscoClient authServiceClient;

  public UcAuthServiceImpl(UcCredentialsRepository repository,
                           VertxCache<String, String> ucTokenCache,
                           UcAuthEbscoClient authServiceClient) {
    this.repository = repository;
    this.ucTokenCache = ucTokenCache;
    this.authServiceClient = authServiceClient;
  }

  @Override
  public CompletableFuture<String> authenticate(Map<String, String> okapiHeaders) {
    return ucTokenCache.getValueOrLoad(TOKEN_CACHE_KEY, () -> loadToken(tenantId(okapiHeaders)));
  }

  @Override
  public CompletableFuture<UCCredentialsClientId> getClientId(Map<String, String> okapiHeaders) {
    return getUcCredentials(tenantId(okapiHeaders))
      .thenApply(DbUcCredentials::getClientId)
      .thenApply(this::mapToClientId);
  }

  @Override
  public CompletableFuture<UCCredentialsClientSecret> getClientSecret(Map<String, String> okapiHeaders) {
    return getUcCredentials(tenantId(okapiHeaders))
      .thenApply(DbUcCredentials::getClientSecret)
      .thenApply(this::mapToClientSecret);
  }

  @Override
  public CompletionStage<UCCredentialsPresence> checkCredentialsPresence(Map<String, String> okapiHeaders) {
    return findUcCredentials(tenantId(okapiHeaders))
      .thenApply(dbUCCredentials -> dbUCCredentials.map(o -> mapToPresence(true)).orElse(mapToPresence(false)));
  }

  @Override
  public CompletionStage<Void> updateCredentials(UCCredentials entity, Map<String, String> okapiHeaders) {
    var credentials = mapToDb(entity);
    return validate(credentials)
      .thenCompose(v -> repository.delete(tenantId(okapiHeaders)))
      .thenCompose(v -> repository.save(credentials, tenantId(okapiHeaders)))
      .thenAccept(v -> ucTokenCache.invalidateAll());
  }

  private CompletableFuture<Void> validate(DbUcCredentials credentials) {
    CompletableFuture<Void> validationFuture = new CompletableFuture<>();
    requestToken(credentials)
      .handle((ucAuthToken, throwable) -> {
        if (throwable != null) {
          Throwable resultException = throwable.getCause();
          if (resultException instanceof UcAuthenticationException) {
            resultException = new InputValidationException(INVALID_UC_CREDENTIALS_MESSAGE, null);
          }
          validationFuture.completeExceptionally(resultException);
        } else {
          validationFuture.complete(null);
        }
        return null;
      });

    return validationFuture;
  }

  private DbUcCredentials mapToDb(UCCredentials entity) {
    var attributes = entity.getAttributes();
    return new DbUcCredentials(attributes.getClientId(), attributes.getClientSecret());
  }

  private CompletableFuture<String> loadToken(String tenantId) {
    return getUcCredentials(tenantId)
      .thenCompose(this::requestToken)
      .thenApply(UcAuthToken::getAccessToken);
  }

  private CompletableFuture<DbUcCredentials> getUcCredentials(String tenantId) {
    return findUcCredentials(tenantId)
      .thenApply(dbUCCredentials -> {
        if (dbUCCredentials.isEmpty()) {
          throw new UcAuthenticationException(INVALID_CREDENTIALS_MESSAGE);
        } else {
          return dbUCCredentials.get();
        }
      });
  }

  private CompletableFuture<UcAuthToken> requestToken(DbUcCredentials credentials) {
    return authServiceClient.requestToken(credentials.getClientId(), credentials.getClientSecret());
  }

  private CompletableFuture<Optional<DbUcCredentials>> findUcCredentials(String tenantId) {
    return repository.find(tenantId);
  }

  private UCCredentialsPresence mapToPresence(boolean isPresent) {
    return new UCCredentialsPresence()
      .withType(UCCredentialsPresence.Type.UC_CREDENTIALS_PRESENCE)
      .withAttributes(new UCCredentialsPresenceAttributes().withIsPresent(isPresent));
  }

  private UCCredentialsClientId mapToClientId(String clientId) {
    return new UCCredentialsClientId()
      .withClientId(clientId);
  }

  private UCCredentialsClientSecret mapToClientSecret(String clientSecret) {
    return new UCCredentialsClientSecret()
      .withClientSecret(clientSecret);
  }
}
