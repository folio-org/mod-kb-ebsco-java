package org.folio.rest.converter.labels;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.CustomLabel;
import org.folio.rest.jaxrs.model.CustomLabelDataAttributes;


public class CustomLabelsConverter {

  private CustomLabelsConverter() {

  }

  @Component
  public static class FromRmApi implements Converter<org.folio.holdingsiq.model.CustomLabel, CustomLabel> {

    @Override
    public CustomLabel convert(org.folio.holdingsiq.model.CustomLabel source) {
      return new CustomLabel()
        .withType(CustomLabel.Type.CUSTOM_LABELS)
        .withAttributes(new CustomLabelDataAttributes()
          .withId(source.getId())
          .withDisplayLabel(source.getDisplayLabel())
          .withDisplayOnFullTextFinder(source.getDisplayOnFullTextFinder())
          .withDisplayOnPublicationFinder(source.getDisplayOnPublicationFinder())
        );
    }
  }

  @Component
  public static class ToRmApi implements Converter<CustomLabel, org.folio.holdingsiq.model.CustomLabel> {

    @Override
    public org.folio.holdingsiq.model.CustomLabel convert(CustomLabel source) {
      CustomLabelDataAttributes attributes = source.getAttributes();
      return org.folio.holdingsiq.model.CustomLabel.builder()
        .id(attributes.getId())
        .displayLabel(attributes.getDisplayLabel())
        .displayOnFullTextFinder(attributes.getDisplayOnFullTextFinder())
        .displayOnPublicationFinder(attributes.getDisplayOnPublicationFinder())
        .build();
    }
  }
}
