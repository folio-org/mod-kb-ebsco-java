package org.folio.rmapi;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.OkapiData;

public class ConfigurationServiceImpl extends org.folio.holdingsiq.service.impl.ConfigurationServiceImpl {

  public ConfigurationServiceImpl(Vertx vertx) {
    super(vertx);
  }

  @Override
  public CompletableFuture<Configuration> retrieveConfiguration(OkapiData okapiData) {
    return super.retrieveConfiguration(okapiData);
  }
}
