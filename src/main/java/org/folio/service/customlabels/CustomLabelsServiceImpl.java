package org.folio.service.customlabels;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.holdingsiq.service.impl.HoldingsIQServiceImpl;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.service.kbcredentials.KbCredentialsService;

@Component
public class CustomLabelsServiceImpl implements CustomLabelsService {

  @Autowired
  private KbCredentialsService credentialsService;

  @Autowired
  private Converter<KbCredentials, Configuration> configurationConverter;
  @Autowired
  private Converter<RootProxyCustomLabels, CustomLabelsCollection> labelsCollectionConverter;

  @Autowired
  private Vertx vertx;

  @Override
  public CompletableFuture<CustomLabelsCollection> fetchCustomLabels(String credentialsId,
                                                                     Map<String, String> okapiHeaders) {
    return credentialsService.findById(credentialsId, false, okapiHeaders)
      .thenApply(configurationConverter::convert)
      .thenCompose(configuration -> new HoldingsIQServiceImpl(configuration, vertx).retrieveRootProxyCustomLabels())
      .thenApply(labelsCollectionConverter::convert)
      .thenApply(customLabelsCollection -> {
        customLabelsCollection.getData().forEach(customLabel -> customLabel.setCredentialsId(credentialsId));
        return customLabelsCollection;
      });
  }

}
