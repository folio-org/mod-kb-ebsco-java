package org.folio.rest.converter.labels;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.CustomLabelPutRequest;

@Component
public class CustomLabelPutRequestToRmApiConverter implements Converter<CustomLabelPutRequest, org.folio.holdingsiq.model.CustomLabel> {

  @Override
  public org.folio.holdingsiq.model.CustomLabel convert(CustomLabelPutRequest customLabelPutRequest) {
    return org.folio.holdingsiq.model.CustomLabel.builder()
      .id(customLabelPutRequest.getData().getAttributes().getId())
      .displayLabel(customLabelPutRequest.getData().getAttributes().getDisplayLabel())
      .displayOnFullTextFinder(customLabelPutRequest.getData().getAttributes().getDisplayOnFullTextFinder())
      .displayOnPublicationFinder(customLabelPutRequest.getData().getAttributes().getDisplayOnPublicationFinder()).build();
  }
}
