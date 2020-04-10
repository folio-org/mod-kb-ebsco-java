package org.folio.rest.converter.labels;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelDataAttributes;

@Component
public class CustomLabelsConverter implements Converter<org.folio.holdingsiq.model.CustomLabel, CustomLabel> {

  @Override
  public CustomLabel convert(@NonNull org.folio.holdingsiq.model.CustomLabel customLabel) {
    return new CustomLabel()
      .withType(CustomLabel.Type.CUSTOM_LABELS)
      .withAttributes(new CustomLabelDataAttributes()
        .withId(customLabel.getId())
        .withDisplayLabel(customLabel.getDisplayLabel())
        .withDisplayOnFullTextFinder(customLabel.getDisplayOnFullTextFinder())
        .withDisplayOnPublicationFinder(customLabel.getDisplayOnPublicationFinder())
      );
  }
}
