package org.folio.service.kbcredentials;

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

@Component
public class CustomLabelsServiceImpl implements CustomLabelsService {

  @Autowired
  private Converter<RootProxyCustomLabels, CustomLabelsCollection> labelsCollectionConverter;

  @Autowired
  private Vertx vertx;

  @Override
  public CompletableFuture<CustomLabelsCollection> fetchCustomLabels(Configuration configuration,
                                                                     Map<String, String> okapiHeaders) {
    return new HoldingsIQServiceImpl(configuration, vertx)
      .retrieveRootProxyCustomLabels()
      .thenApply(labelsCollectionConverter::convert);
  }

}
