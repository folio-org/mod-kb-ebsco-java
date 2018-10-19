package org.folio.config;

import io.vertx.core.Context;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.config.cache.RMAPIConfigurationCache;
import org.folio.rest.model.OkapiData;

import java.util.concurrent.CompletableFuture;

public class RMAPIConfigurationServiceCache implements RMAPIConfigurationService {

  private RMAPIConfigurationService rmapiConfigurationService;

  public RMAPIConfigurationServiceCache(RMAPIConfigurationService rmapiConfigurationService) {
    this.rmapiConfigurationService = rmapiConfigurationService;
  }

  @Override
  public CompletableFuture<RMAPIConfiguration> retrieveConfiguration(OkapiData okapiData) {
    RMAPIConfiguration cachedConfiguration = RMAPIConfigurationCache.getInstance()
      .getValue();
    if(cachedConfiguration != null){
      return CompletableFuture.completedFuture(cachedConfiguration);
    }

    return rmapiConfigurationService.retrieveConfiguration(okapiData)
    .thenCompose(rmapiConfiguration -> {
      RMAPIConfigurationCache.getInstance()
        .putValue(rmapiConfiguration);
      return CompletableFuture.completedFuture(rmapiConfiguration);
    });
  }

  @Override
  public CompletableFuture<RMAPIConfiguration> updateConfiguration(RMAPIConfiguration rmapiConfiguration, OkapiData okapiData) {
    return rmapiConfigurationService.updateConfiguration(rmapiConfiguration, okapiData)
    .thenCompose(configuration ->  {
      RMAPIConfigurationCache.getInstance()
        .putValue(configuration);
      return CompletableFuture.completedFuture(configuration);
    });
  }

  @Override
  public CompletableFuture<Boolean> verifyCredentials(RMAPIConfiguration rmapiConfiguration, Context vertxContext) {
    if(rmapiConfiguration.getValid() != null){
      return CompletableFuture.completedFuture(rmapiConfiguration.getValid());
    }
    return rmapiConfigurationService.verifyCredentials(rmapiConfiguration, vertxContext)
      .thenCompose(isValid -> {
        rmapiConfiguration.setValid(isValid);
        return CompletableFuture.completedFuture(isValid);
      });
  }
}
