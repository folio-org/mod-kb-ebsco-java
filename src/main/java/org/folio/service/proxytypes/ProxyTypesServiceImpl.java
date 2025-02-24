package org.folio.service.proxytypes;

import io.vertx.core.Vertx;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.Proxies;
import org.folio.holdingsiq.service.impl.HoldingsIQServiceImpl;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.ProxyTypes;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ProxyTypesServiceImpl implements ProxyTypesService {

  private final KbCredentialsService credentialsService;

  private final Converter<KbCredentials, Configuration> configurationConverter;

  private final Converter<Proxies, ProxyTypes> proxyTypesConverter;

  private final Vertx vertx;

  public ProxyTypesServiceImpl(@Qualifier("nonSecuredCredentialsService") KbCredentialsService credentialsService,
                               Converter<KbCredentials, Configuration> configurationConverter,
                               Converter<Proxies, ProxyTypes> proxyTypesConverter, Vertx vertx) {
    this.credentialsService = credentialsService;
    this.configurationConverter = configurationConverter;
    this.proxyTypesConverter = proxyTypesConverter;
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<ProxyTypes> findByCredentialsId(String credentialsId, Map<String, String> okapiHeaders) {
    return credentialsService.findById(credentialsId, okapiHeaders)
      .thenCompose(this::retrieveProxies);
  }

  @Override
  public CompletableFuture<ProxyTypes> findByUser(Map<String, String> okapiHeaders) {
    return credentialsService.findByUser(okapiHeaders)
      .thenCompose(this::retrieveProxies);
  }

  private CompletableFuture<ProxyTypes> retrieveProxies(KbCredentials kbCredentials) {
    final HoldingsIQServiceImpl holdingsIqService = createHoldingsIqService(kbCredentials);
    return holdingsIqService.retrieveProxies()
      .thenApply(proxyTypesConverter::convert)
      .thenApply(proxyTypes -> setCredentialsId(proxyTypes, kbCredentials.getId()));
  }

  private ProxyTypes setCredentialsId(ProxyTypes proxyTypes, String credentialsId) {
    proxyTypes.getData().forEach(proxy -> proxy.setCredentialsId(credentialsId));
    return proxyTypes;
  }

  private HoldingsIQServiceImpl createHoldingsIqService(KbCredentials credentials) {
    final Configuration rmApiConfig = configurationConverter.convert(credentials);
    return new HoldingsIQServiceImpl(Objects.requireNonNull(rmApiConfig), vertx);
  }
}
