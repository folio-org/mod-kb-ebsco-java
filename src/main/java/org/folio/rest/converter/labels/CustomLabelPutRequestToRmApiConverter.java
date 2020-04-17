package org.folio.rest.converter.labels;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.CustomLabel;
import org.folio.holdingsiq.model.RootProxyCustomLabels;
import org.folio.rest.jaxrs.model.CustomLabelDataAttributes;
import org.folio.rest.jaxrs.model.CustomLabelPutRequest;

@Component
public class CustomLabelPutRequestToRmApiConverter implements Converter<CustomLabelPutRequest, RootProxyCustomLabels> {

  @Override
  public RootProxyCustomLabels convert(@NonNull CustomLabelPutRequest customLabelPutRequest) {
    return RootProxyCustomLabels.builder()
      .labelList(toCustomLabelList(customLabelPutRequest))
      .build();
  }

  private List<CustomLabel> toCustomLabelList(CustomLabelPutRequest customLabelPutRequest) {
    return mapItems(customLabelPutRequest.getData(), this::toCustomLabel);
  }

  private CustomLabel toCustomLabel(org.folio.rest.jaxrs.model.CustomLabel customLabel) {
    CustomLabelDataAttributes attributes = customLabel.getAttributes();
    return CustomLabel.builder()
      .id(attributes.getId())
      .displayLabel(attributes.getDisplayLabel())
      .displayOnFullTextFinder(attributes.getDisplayOnFullTextFinder())
      .displayOnPublicationFinder(attributes.getDisplayOnPublicationFinder())
      .build();
  }
}
