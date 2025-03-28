package org.folio.service.rootproxies;

import io.vertx.core.Vertx;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.holdingsiq.service.impl.HoldingsIQServiceImpl;
import org.folio.rest.converter.proxy.RootProxyPutConverter;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.jaxrs.model.RootProxy;
import org.folio.rest.jaxrs.model.RootProxyPutRequest;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class RootProxyServiceImpl implements RootProxyService {

  private final KbCredentialsService credentialsService;
  private final Converter<KbCredentials, Configuration> configurationConverter;
  private final Converter<RootProxyCustomLabels, RootProxy> rootProxyConverter;
  private final RootProxyPutConverter putRootProxyConverter;
  private final Vertx vertx;

  public RootProxyServiceImpl(@Qualifier("nonSecuredCredentialsService") KbCredentialsService credentialsService,
                              Converter<KbCredentials, Configuration> configurationConverter,
                              Converter<RootProxyCustomLabels, RootProxy> rootProxyConverter,
                              RootProxyPutConverter putRootProxyConverter, Vertx vertx) {
    this.credentialsService = credentialsService;
    this.configurationConverter = configurationConverter;
    this.rootProxyConverter = rootProxyConverter;
    this.putRootProxyConverter = putRootProxyConverter;
    this.vertx = vertx;
  }

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
  public CompletableFuture<RootProxy> updateRootProxy(String credentialsId, RootProxyPutRequest entity,
                                                      Map<String, String> okapiHeaders) {
    return credentialsService.findById(credentialsId, okapiHeaders)
      .thenCompose(kbCredentials -> updateRootProxy(kbCredentials, entity));
  }

  private CompletableFuture<RootProxy> updateRootProxy(KbCredentials kbCredentials, RootProxyPutRequest entity) {
    final HoldingsIQServiceImpl holdingsIqService = createHoldingsIqService(kbCredentials);
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
    final HoldingsIQServiceImpl holdingsIqService = createHoldingsIqService(kbCredentials);
    return holdingsIqService.retrieveRootProxyCustomLabels()
      .thenApply(rootProxyConverter::convert)
      .thenApply(rootProxy -> {
        rootProxy.getData().setCredentialsId(kbCredentials.getId());
        return rootProxy;
      });
  }

  private HoldingsIQServiceImpl createHoldingsIqService(KbCredentials credentials) {
    final Configuration rmApiConfig = configurationConverter.convert(credentials);
    return new HoldingsIQServiceImpl(Objects.requireNonNull(rmApiConfig), vertx);
  }
}
