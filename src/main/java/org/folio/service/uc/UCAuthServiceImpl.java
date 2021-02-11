package org.folio.service.uc;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import org.folio.cache.VertxCache;
import org.folio.client.uc.UCAuthEbscoClient;
import org.folio.client.uc.model.UCAuthToken;
import org.folio.repository.uc.DbUCCredentials;
import org.folio.repository.uc.UCCredentialsRepository;
import org.folio.rest.tools.utils.TenantTool;

@Service
public class UCAuthServiceImpl implements UCAuthService {

  public static final String TOKEN_CACHE_KEY = "TOKEN_KEY";
  public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid UC API Credentials";

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
    return ucTokenCache.getValueOrLoad(TOKEN_CACHE_KEY, () -> loadToken(TenantTool.tenantId(okapiHeaders)));
  }

  private CompletableFuture<String> loadToken(String tenantId) {
    return repository.find(tenantId)
      .thenCompose(dbUCCredentials -> {
        if (dbUCCredentials.isEmpty()) {
          throw new UcAuthenticationException(INVALID_CREDENTIALS_MESSAGE);
        } else {
          DbUCCredentials credentials = dbUCCredentials.get();
          return authServiceClient.requestToken(credentials.getClientId(), credentials.getClientSecret());
        }
      })
      .thenApply(UCAuthToken::getAccessToken);
  }
}
