package org.folio.rest.converter.labels;


import static org.folio.rest.converter.labels.CustomLabelsItemConverter.CUSTOM_LABEL_TYPE;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelCollectionItem;
import org.folio.rest.jaxrs.model.CustomLabelDataAttributes;
import org.folio.rest.jaxrs.model.CustomLabelPutRequest;
import org.folio.rest.util.RestConstants;

@Component
public class CustomLabelPutRequestConverter implements Converter<CustomLabelPutRequest, CustomLabel> {
  @Override
  public CustomLabel convert(CustomLabelPutRequest customLabelPutRequest) {
    return new CustomLabel()
      .withData(new CustomLabelCollectionItem()
        .withType(CUSTOM_LABEL_TYPE)
        .withAttributes(new CustomLabelDataAttributes()
          .withId(customLabelPutRequest.getData().getAttributes().getId())
          .withDisplayLabel(customLabelPutRequest.getData().getAttributes().getDisplayLabel())
          .withDisplayOnFullTextFinder(customLabelPutRequest.getData().getAttributes().getDisplayOnFullTextFinder())
          .withDisplayOnPublicationFinder(customLabelPutRequest.getData().getAttributes().getDisplayOnPublicationFinder())))
      .withJsonapi(RestConstants.JSONAPI);
  }
}
