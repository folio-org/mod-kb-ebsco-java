package org.folio.config.api;

import io.vertx.core.Context;
import org.folio.config.RMAPIConfiguration;
import org.folio.config.model.ConfigurationError;
import org.folio.rest.model.OkapiData;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface RMAPIConfigurationService {
  CompletableFuture<RMAPIConfiguration> retrieveConfiguration(OkapiData okapiData);
  CompletableFuture<RMAPIConfiguration> updateConfiguration(RMAPIConfiguration rmapiConfiguration, OkapiData okapiData);
  CompletableFuture<List<ConfigurationError>> verifyCredentials(RMAPIConfiguration rmapiConfiguration, Context vertxContext, String tenant);
}
