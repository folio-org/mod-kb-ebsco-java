package org.folio.rest.converter.labels;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.CustomLabel;
import org.folio.rest.util.RestConstants;

@Component
public class CustomLabelConverter implements Converter<org.folio.holdingsiq.model.CustomLabel, org.folio.rest.jaxrs.model.CustomLabel> {

  @Autowired
  private CustomLabelsItemConverter itemConverter;

  @Override
  public org.folio.rest.jaxrs.model.CustomLabel convert(@NonNull CustomLabel customLabel) {
    return new org.folio.rest.jaxrs.model.CustomLabel()
      .withData(itemConverter.convert(customLabel))
      .withJsonapi(RestConstants.JSONAPI);
  }
}
