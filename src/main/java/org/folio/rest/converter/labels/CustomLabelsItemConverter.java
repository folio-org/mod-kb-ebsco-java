package org.folio.rest.converter.labels;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelCollectionItem;
import org.folio.rest.jaxrs.model.CustomLabelDataAttributes;

@Component
public class CustomLabelsItemConverter implements Converter<CustomLabel, CustomLabelCollectionItem> {

  private static final String CUSTOM_LABEL_TYPE = "customLabel";

  @Override
  public CustomLabelCollectionItem convert(@NonNull CustomLabel customLabel) {
    return new CustomLabelCollectionItem()
      .withType(CUSTOM_LABEL_TYPE)
      .withAttributes(
        new CustomLabelDataAttributes()
          .withId(customLabel.getId())
          .withDisplayLabel(customLabel.getDisplayLabel())
          .withDisplayOnFullTextFinder(customLabel.getDisplayOnFullTextFinder())
          .withDisplayOnPublicationFinder(customLabel.getDisplayOnPublicationFinder())
      );
  }
}
