package org.folio.rest.converter.labels;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.CustomLabel;
import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.jaxrs.model.CustomLabelCollectionItem;
import org.folio.rest.jaxrs.model.CustomLabelDataAttributes;
import org.folio.rest.jaxrs.model.CustomLabelPutRequest;

@Component
public class CustomLabelPutRequestToRmApiConverter implements Converter<CustomLabelPutRequest, RootProxyCustomLabels> {

  @Override
  public RootProxyCustomLabels convert(CustomLabelPutRequest customLabelPutRequest) {
    List<CustomLabelCollectionItem> customLabelItems = customLabelPutRequest.getData();
    List<CustomLabel> labelList = customLabelItems.stream()
        .map(this::toCustomLabel)
        .collect(Collectors.toList());
    return RootProxyCustomLabels.builder()
        .labelList(labelList)
        .build();
  }

  private CustomLabel toCustomLabel(CustomLabelCollectionItem customLabelCollectionItem) {
    CustomLabelDataAttributes attributes = customLabelCollectionItem.getAttributes();
    return CustomLabel.builder()
        .id(attributes.getId())
        .displayLabel(attributes.getDisplayLabel())
        .displayOnFullTextFinder(attributes.getDisplayOnFullTextFinder())
        .displayOnPublicationFinder(attributes.getDisplayOnPublicationFinder())
        .build();
  }
}
