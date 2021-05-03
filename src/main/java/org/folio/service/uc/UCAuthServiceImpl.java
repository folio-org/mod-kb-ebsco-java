package org.folio.service.uc;

import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.springframework.stereotype.Service;

import org.folio.cache.VertxCache;
import org.folio.client.uc.UCAuthEbscoClient;
import org.folio.client.uc.model.UCAuthToken;
import org.folio.repository.uc.DbUCCredentials;
import org.folio.repository.uc.UCCredentialsRepository;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.UCCredentials;
import org.folio.rest.jaxrs.model.UCCredentialsPresence;
import org.folio.rest.jaxrs.model.UCCredentialsPresenceAttributes;

@Service
public class UCAuthServiceImpl implements UCAuthService {

  public static final String TOKEN_CACHE_KEY = "TOKEN_KEY";
  public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid UC API Credentials";
  public static final String INVALID_UC_CREDENTIALS_MESSAGE = "Invalid Usage Consolidation Credentials";

  private final UCCredentialsRepository repository;
  private final VertxCache<String, String> ucTokenCache;
  private final UCAuthEbscoClient authServiceClient;

  public UCAuthServiceImpl(UCCredentialsRepository repository,
                           VertxCache<String, String> ucTokenCache,
                           UCAuthEbscoClient authServiceClient) {
    this.repository = repository;
    this.ucTokenCache = ucTokenCache;
    this.authServiceClient = authServiceClient;
  }

  @Override
  public CompletableFuture<String> authenticate(Map<String, String> okapiHeaders) {
    return ucTokenCache.getValueOrLoad(TOKEN_CACHE_KEY, () -> loadToken(tenantId(okapiHeaders)));
  }

  @Override
  public CompletionStage<UCCredentialsPresence> checkCredentialsPresence(Map<String, String> okapiHeaders) {
    return findUCCredentials(tenantId(okapiHeaders))
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

  private CompletableFuture<Void> validate(DbUCCredentials credentials) {
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

  private DbUCCredentials mapToDb(UCCredentials entity) {
    var attributes = entity.getAttributes();
    return new DbUCCredentials(attributes.getClientId(), attributes.getClientSecret());
  }

  private CompletableFuture<String> loadToken(String tenantId) {
    return findUCCredentials(tenantId)
      .thenCompose(dbUCCredentials -> {
        if (dbUCCredentials.isEmpty()) {
          throw new UcAuthenticationException(INVALID_CREDENTIALS_MESSAGE);
        } else {
          return requestToken(dbUCCredentials.get());
        }
      })
      .thenApply(UCAuthToken::getAccessToken);
  }

  private CompletableFuture<UCAuthToken> requestToken(DbUCCredentials credentials) {
    return authServiceClient.requestToken(credentials.getClientId(), credentials.getClientSecret());
  }

  private CompletableFuture<Optional<DbUCCredentials>> findUCCredentials(String tenantId) {
    return repository.find(tenantId);
  }

  private UCCredentialsPresence mapToPresence(boolean isPresent) {
    return new UCCredentialsPresence()
      .withType(UCCredentialsPresence.Type.UC_CREDENTIALS_PRESENCE)
      .withAttributes(new UCCredentialsPresenceAttributes().withIsPresent(isPresent));
  }
}
