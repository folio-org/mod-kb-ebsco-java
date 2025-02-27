package org.folio.service.customlabels;

import static org.folio.common.ListUtils.mapItems;
import static org.folio.common.LogUtils.collectionToLogMsg;
import static org.folio.rest.util.RequestHeadersUtil.tenantId;

import io.vertx.core.Vertx;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.extern.log4j.Log4j2;
import org.folio.holdingsiq.model.Configuration;
import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.holdingsiq.service.impl.HoldingsIQServiceImpl;
import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelsCollection;
import org.folio.rest.jaxrs.model.CustomLabelsPutRequest;
import org.folio.rest.jaxrs.model.KbCredentials;
import org.folio.rest.validator.CustomLabelsPutBodyValidator;
import org.folio.service.kbcredentials.KbCredentialsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class CustomLabelsServiceImpl implements CustomLabelsService {

  private final KbCredentialsService credentialsService;

  private final CustomLabelsPutBodyValidator validator;

  private final Converter<KbCredentials, Configuration> configurationConverter;
  private final Converter<List<CustomLabel>, CustomLabelsCollection> fromListConverter;
  private final Converter<RootProxyCustomLabels, CustomLabelsCollection> fromRmApiConverter;
  private final Converter<CustomLabel, org.folio.holdingsiq.model.CustomLabel> labelConverter;

  private final Vertx vertx;

  public CustomLabelsServiceImpl(@Qualifier("nonSecuredCredentialsService") KbCredentialsService credentialsService,
                                 CustomLabelsPutBodyValidator validator,
                                 Converter<KbCredentials, Configuration> configurationConverter,
                                 Converter<List<CustomLabel>, CustomLabelsCollection> fromListConverter,
                                 Converter<RootProxyCustomLabels, CustomLabelsCollection> fromRmApiConverter,
                                 Converter<CustomLabel, org.folio.holdingsiq.model.CustomLabel> labelConverter,
                                 Vertx vertx) {
    this.credentialsService = credentialsService;
    this.validator = validator;
    this.configurationConverter = configurationConverter;
    this.fromListConverter = fromListConverter;
    this.fromRmApiConverter = fromRmApiConverter;
    this.labelConverter = labelConverter;
    this.vertx = vertx;
  }

  @Override
  public CompletableFuture<CustomLabelsCollection> fetch(String credentialsId,
                                                         Map<String, String> okapiHeaders) {
    log.debug("fetch:: by [tenant: {}]", tenantId(okapiHeaders));

    return createHoldingsIqService(credentialsId, okapiHeaders)
      .thenCompose(HoldingsIQServiceImpl::retrieveRootProxyCustomLabels)
      .thenApply(fromRmApiConverter::convert)
      .thenApply(customLabelsCollection -> setCredentialsId(customLabelsCollection, credentialsId));
  }

  @Override
  public CompletableFuture<CustomLabelsCollection> update(String credentialsId, CustomLabelsPutRequest putRequest,
                                                          Map<String, String> okapiHeaders) {
    log.debug("update:: by [tenant: {}, customLabels: {}]", tenantId(okapiHeaders),
      collectionToLogMsg(putRequest.getData()));

    validator.validate(putRequest);
    List<CustomLabel> requestData = putRequest.getData();
    return createHoldingsIqService(credentialsId, okapiHeaders)
      .thenCompose(holdingsIQService -> updateLabels(requestData, holdingsIQService))
      .thenApply(o -> fromListConverter.convert(requestData))
      .thenApply(customLabelsCollection -> setCredentialsId(customLabelsCollection, credentialsId));
  }

  private CompletableFuture<RootProxyCustomLabels> updateLabels(List<CustomLabel> requestData,
                                                                HoldingsIQServiceImpl holdingsIqService) {
    return holdingsIqService.retrieveRootProxyCustomLabels()
      .thenApply(target -> {
        target.getLabelList().clear();
        target.getLabelList().addAll(mapItems(requestData, labelConverter::convert));
        return target;
      })
      .thenCompose(holdingsIqService::updateRootProxyCustomLabels);
  }

  private CompletableFuture<HoldingsIQServiceImpl> createHoldingsIqService(String credentialsId,
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
