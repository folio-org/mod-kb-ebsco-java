package org.folio.rest.converter;

import org.folio.rest.jaxrs.model.ConfigurationStatus;
import org.folio.rest.jaxrs.model.StatusAttributes;
import org.folio.rest.jaxrs.model.StatusData;
import org.folio.rest.util.RestConstants;
import org.springframework.stereotype.Component;

@Component
public class StatusConverter {
  public ConfigurationStatus convert(boolean isValid){
    return new ConfigurationStatus()
      .withData(new StatusData()
        .withId("status")
        .withType("statuses")
        .withAttributes(new StatusAttributes()
          .withIsConfigurationValid(isValid)))
      .withJsonapi(RestConstants.JSONAPI);
  }
}
