package org.folio.service.rootproxies;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.vertx.core.Vertx;
import org.folio.rest.converter.proxy.RootProxyPutConverter;
import org.folio.rest.jaxrs.model.RootProxyPutRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.holdingsiq.service.impl.HoldingsIQServiceImpl;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.RootProxy;
import org.folio.service.kbcredentials.KbCredentialsService;

@Component
public class RootProxyServiceImpl implements RootProxyService {

  @Autowired
  @Qualifier("nonSecuredCredentialsService")
  private KbCredentialsService credentialsService;
  @Autowired
  private Converter<KbCredentials, Configuration> configurationConverter;
  @Autowired
  private Converter<RootProxyCustomLabels, RootProxy> rootProxyConverter;
  @Autowired
  private RootProxyPutConverter putRootProxyConverter;
  @Autowired
  private Vertx vertx;

  @Override
  public CompletableFuture<RootProxy> findByCredentialsId(String credentialsId, Map<String, String> okapiHeaders) {
    return credentialsService.findById(credentialsId, okapiHeaders)
      .thenCompose(this::retrieveRootProxy);
  }

  @Override
  public CompletableFuture<RootProxy> findByUser(Map<String, String> okapiHeaders) {
    return credentialsService.findByUser(okapiHeaders)
      .thenCompose(this::retrieveRootProxy);
  }

  @Override
  public CompletionStage<RootProxy> updateRootProxy(String credentialsId, RootProxyPutRequest entity, Map<String, String> okapiHeaders) {
    return credentialsService.findById(credentialsId, okapiHeaders)
      .thenCompose(kbCredentials -> updateRootProxy(kbCredentials, entity));
  }

  private CompletableFuture<RootProxy> updateRootProxy(KbCredentials kbCredentials, RootProxyPutRequest entity) {
    final HoldingsIQServiceImpl holdingsIqService = createHoldingsIQService(kbCredentials);
    return holdingsIqService.retrieveRootProxyCustomLabels()
      .thenApply(rootProxyLabels -> putRootProxyConverter.convertToRootProxyCustomLabels(entity, rootProxyLabels))
      .thenCompose(holdingsIqService::updateRootProxyCustomLabels)
      .thenApply(rootProxyConverter::convert)
      .thenApply(rootProxy -> {
        rootProxy.getData().setCredentialsId(kbCredentials.getId());
        return rootProxy;
      });
  }

  private CompletableFuture<RootProxy> retrieveRootProxy(KbCredentials kbCredentials) {
    final HoldingsIQServiceImpl holdingsIqService = createHoldingsIQService(kbCredentials);
    return holdingsIqService.retrieveRootProxyCustomLabels()
      .thenApply(rootProxyConverter::convert)
      .thenApply(rootProxy -> {
        rootProxy.getData().setCredentialsId(kbCredentials.getId());
        return rootProxy;
      });
  }

  private HoldingsIQServiceImpl createHoldingsIQService(KbCredentials credentials) {
    final Configuration rmApiConfig = configurationConverter.convert(credentials);
    return new HoldingsIQServiceImpl(Objects.requireNonNull(rmApiConfig), vertx);
  }
}
