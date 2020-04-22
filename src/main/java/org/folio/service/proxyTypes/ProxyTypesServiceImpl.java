package org.folio.service.proxyTypes;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.Proxies;
import org.folio.holdingsiq.service.impl.HoldingsIQServiceImpl;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.ProxyTypes;
import org.folio.service.kbcredentials.KbCredentialsService;

@Component
public class ProxyTypesServiceImpl implements ProxyTypesService {

  @Autowired
  @Qualifier("nonSecuredCredentialsService")
  private KbCredentialsService credentialsService;

  @Autowired
  private Converter<KbCredentials, Configuration> configurationConverter;

  @Autowired
  private Converter<Proxies, ProxyTypes> proxyTypesConverter;

  @Autowired
  private Vertx vertx;

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
    final HoldingsIQServiceImpl holdingsIQService = createHoldingsIQService(kbCredentials);
    return holdingsIQService.retrieveProxies()
      .thenApply(proxyTypesConverter:: convert)
      .thenApply(proxyTypes -> setCredentialsId(proxyTypes, kbCredentials.getId()));
  }

  private ProxyTypes setCredentialsId(ProxyTypes proxyTypes, String credentialsId) {
    proxyTypes.getData().forEach(proxy -> proxy.setCredentialsId(credentialsId));
    return proxyTypes;
  }

  private HoldingsIQServiceImpl createHoldingsIQService(KbCredentials credentials) {
    final Configuration rmApiConfig = configurationConverter.convert(credentials);
    return new HoldingsIQServiceImpl(Objects.requireNonNull(rmApiConfig), vertx);
  }
}
