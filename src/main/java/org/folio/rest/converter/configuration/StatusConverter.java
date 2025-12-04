package org.folio.rest.converter.configuration;

import org.folio.rest.jaxrs.model.ConfigurationStatus;
import org.folio.rest.jaxrs.model.StatusAttributes;
import org.folio.rest.jaxrs.model.StatusData;
import org.folio.rest.util.RestConstants;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StatusConverter implements Converter<@NonNull Boolean, @NonNull ConfigurationStatus> {

  @Override
  public ConfigurationStatus convert(Boolean isValid) {
    return new ConfigurationStatus()
      .withData(new StatusData()
        .withId("status")
        .withType("statuses")
        .withAttributes(new StatusAttributes()
          .withIsConfigurationValid(isValid)))
      .withJsonapi(RestConstants.JSONAPI);
  }
}
