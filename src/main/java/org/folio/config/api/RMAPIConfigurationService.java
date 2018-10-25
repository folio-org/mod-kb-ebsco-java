package org.folio.config.api;

import io.vertx.core.Context;
import org.folio.config.RMAPIConfiguration;
import org.folio.rest.model.OkapiData;

import java.util.concurrent.CompletableFuture;

public interface RMAPIConfigurationService {
  CompletableFuture<RMAPIConfiguration> retrieveConfiguration(OkapiData okapiData);
  CompletableFuture<RMAPIConfiguration> updateConfiguration(RMAPIConfiguration rmapiConfiguration, OkapiData okapiData);
  CompletableFuture<Boolean> verifyCredentials(RMAPIConfiguration rmapiConfiguration, Context vertxContext);
}
