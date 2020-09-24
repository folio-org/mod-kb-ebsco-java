package org.folio.service.uc;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import org.folio.cache.VertxCache;
import org.folio.client.uc.UCAuthServiceClient;
import org.folio.client.uc.UCAuthToken;
import org.folio.repository.uc.DbUCCredentials;
import org.folio.repository.uc.UCCredentialsRepository;
import org.folio.rest.tools.utils.TenantTool;

@Service
public class UCAuthServiceImpl implements UCAuthService {

  public static final String TOKEN_CACHE_KEY = "TOKEN_KEY";

  private final UCCredentialsRepository repository;
  private final VertxCache<String, String> ucTokenCache;
  private final UCAuthServiceClient authServiceClient;

  public UCAuthServiceImpl(UCCredentialsRepository repository,
                           VertxCache<String, String> ucTokenCache,
                           UCAuthServiceClient authServiceClient) {
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
          throw new UcAuthenticationException("UC Credentials are not exist in database.");
        } else {
          DbUCCredentials credentials = dbUCCredentials.get();
          return authServiceClient.requestToken(credentials.getClientId(), credentials.getClientSecret());
        }
      })
      .thenApply(UCAuthToken::getAccessToken);
  }
}
