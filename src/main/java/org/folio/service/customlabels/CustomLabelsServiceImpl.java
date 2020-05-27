package org.folio.service.customlabels;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.holdingsiq.service.impl.HoldingsIQServiceImpl;
import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.model.CustomLabelsPutRequest;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.validator.CustomLabelsPutBodyValidator;
import org.folio.service.kbcredentials.KbCredentialsService;

@Component
public class CustomLabelsServiceImpl implements CustomLabelsService {

  @Autowired
  @Qualifier("nonSecuredCredentialsService")
  private KbCredentialsService credentialsService;

  @Autowired
  private CustomLabelsPutBodyValidator validator;

  @Autowired
  private Converter<KbCredentials, Configuration> configurationConverter;
  @Autowired
  private Converter<List<CustomLabel>, CustomLabelsCollection> fromListConverter;
  @Autowired
  private Converter<RootProxyCustomLabels, CustomLabelsCollection> fromRmApiConverter;
  @Autowired
  private Converter<CustomLabel, org.folio.holdingsiq.model.CustomLabel> labelConverter;

  @Autowired
  private Vertx vertx;

  @Override
  public CompletableFuture<CustomLabelsCollection> fetch(String credentialsId,
                                                         Map<String, String> okapiHeaders) {
    return createHoldingsIQService(credentialsId, okapiHeaders)
      .thenCompose(HoldingsIQServiceImpl::retrieveRootProxyCustomLabels)
      .thenApply(fromRmApiConverter::convert)
      .thenApply(customLabelsCollection -> setCredentialsId(customLabelsCollection, credentialsId));
  }

  @Override
  public CompletableFuture<CustomLabelsCollection> update(String credentialsId, CustomLabelsPutRequest putRequest,
                                                          Map<String, String> okapiHeaders) {
    validator.validate(putRequest);
    List<CustomLabel> requestData = putRequest.getData();
    return createHoldingsIQService(credentialsId, okapiHeaders)
      .thenCompose(holdingsIQService -> updateLabels(requestData, holdingsIQService))
      .thenApply(o -> fromListConverter.convert(requestData))
      .thenApply(customLabelsCollection -> setCredentialsId(customLabelsCollection, credentialsId));
  }

  private CompletableFuture<RootProxyCustomLabels> updateLabels(List<CustomLabel> requestData,
                                                                HoldingsIQServiceImpl holdingsIQService) {
    return holdingsIQService.retrieveRootProxyCustomLabels()
      .thenApply(target -> {
        target.getLabelList().clear();
        target.getLabelList().addAll(mapItems(requestData, labelConverter::convert));
        return target;
      })
      .thenCompose(holdingsIQService::updateRootProxyCustomLabels);
  }

  private CompletableFuture<HoldingsIQServiceImpl> createHoldingsIQService(String credentialsId,
                                                                           Map<String, String> okapiHeaders) {
    return credentialsService.findById(credentialsId, okapiHeaders)
      .thenApply(configurationConverter::convert)
      .thenApply(configuration -> new HoldingsIQServiceImpl(configuration, vertx));
  }

  private CustomLabelsCollection setCredentialsId(CustomLabelsCollection customLabelsCollection, String credentialsId) {
    customLabelsCollection.getData().forEach(customLabel -> customLabel.setCredentialsId(credentialsId));
    return customLabelsCollection;
  }
}
