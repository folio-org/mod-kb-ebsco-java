package org.folio.rest.converter.configuration;

import org.folio.rest.jaxrs.model.ConfigurationStatus;
import org.folio.rest.jaxrs.model.StatusAttributes;
import org.folio.rest.jaxrs.model.StatusData;
import org.folio.rest.util.RestConstants;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class StatusConverter implements Converter<Boolean, ConfigurationStatus> {

  @Override
  public ConfigurationStatus convert(@NonNull Boolean isValid) {
    return new ConfigurationStatus()
      .withData(new StatusData()
        .withId("status")
        .withType("statuses")
        .withAttributes(new StatusAttributes()
          .withIsConfigurationValid(isValid)))
      .withJsonapi(RestConstants.JSONAPI);
  }
}
