package org.folio.config.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.folio.config.RMAPIConfiguration;
import org.folio.config.api.RMAPIConfigurationService;
import org.folio.config.cache.RMAPIConfigurationCache;
import org.folio.config.model.ConfigurationError;
import org.folio.rest.model.OkapiData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import io.vertx.core.Context;

@Component
@Primary
public class RMAPIConfigurationServiceCache implements RMAPIConfigurationService {

  @Qualifier("rmAPIConfigurationServiceImpl")
  private RMAPIConfigurationService rmapiConfigurationService;

  public RMAPIConfigurationServiceCache(RMAPIConfigurationService rmapiConfigurationService) {
    this.rmapiConfigurationService = rmapiConfigurationService;
  }

  @Override
  public CompletableFuture<RMAPIConfiguration> retrieveConfiguration(OkapiData okapiData, Context vertxContext) {
    RMAPIConfiguration cachedConfiguration = new RMAPIConfigurationCache(vertxContext.owner())
      .getValue(okapiData.getTenant());
    if(cachedConfiguration != null){
      return CompletableFuture.completedFuture(cachedConfiguration);
    }

    return rmapiConfigurationService.retrieveConfiguration(okapiData, vertxContext)
    .thenCompose(rmapiConfiguration -> {
      new RMAPIConfigurationCache(vertxContext.owner())
        .putValue(okapiData.getTenant(), rmapiConfiguration);
      return CompletableFuture.completedFuture(rmapiConfiguration);
    });
  }

  @Override
  public CompletableFuture<RMAPIConfiguration> updateConfiguration(RMAPIConfiguration rmapiConfiguration, Context vertxContext, OkapiData okapiData) {
    return rmapiConfigurationService.updateConfiguration(rmapiConfiguration, vertxContext, okapiData)
    .thenCompose(configuration ->  {
      new RMAPIConfigurationCache(vertxContext.owner())
        .putValue(okapiData.getTenant(), configuration);
      return CompletableFuture.completedFuture(configuration);
    });
  }

  @Override
  public CompletableFuture<List<ConfigurationError>> verifyCredentials(RMAPIConfiguration rmapiConfiguration, Context vertxContext, String tenant) {
    if(rmapiConfiguration.getConfigValid() != null && rmapiConfiguration.getConfigValid()){
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
    return rmapiConfigurationService.verifyCredentials(rmapiConfiguration, vertxContext, tenant)
      .thenCompose(errors -> {
        if(errors.isEmpty()){
          new RMAPIConfigurationCache(vertxContext.owner()).putValue(tenant,
            rmapiConfiguration.toBuilder().configValid(true).build());
        }
        return CompletableFuture.completedFuture(errors);
      });
  }

}
