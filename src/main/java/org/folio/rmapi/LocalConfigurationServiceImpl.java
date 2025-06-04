package org.folio.rmapi;

import io.vertx.core.Vertx;
import java.util.concurrent.CompletableFuture;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.OkapiData;
import org.folio.holdingsiq.service.impl.ConfigurationServiceImpl;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.service.kbcredentials.UserKbCredentialsService;
import org.springframework.core.convert.converter.Converter;

public class LocalConfigurationServiceImpl extends ConfigurationServiceImpl {

  private final UserKbCredentialsService userKbCredentialsService;
  private final Converter<KbCredentials, Configuration> converter;

  public LocalConfigurationServiceImpl(UserKbCredentialsService userKbCredentialsService,
                                       Converter<KbCredentials, Configuration> converter, Vertx vertx) {
    super(vertx);
    this.userKbCredentialsService = userKbCredentialsService;
    this.converter = converter;
  }

  @Override
  public CompletableFuture<Configuration> retrieveConfiguration(OkapiData okapiData) {
    return userKbCredentialsService.findByUser(okapiData.getHeaders()).thenApply(converter::convert);
  }
}
