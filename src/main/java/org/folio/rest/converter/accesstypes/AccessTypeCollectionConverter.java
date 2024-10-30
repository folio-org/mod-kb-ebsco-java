package org.folio.rest.converter.accesstypes;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.util.RestConstants;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AccessTypeCollectionConverter implements Converter<List<AccessType>, AccessTypeCollection> {

  @Override
  public AccessTypeCollection convert(@NotNull List<AccessType> source) {
    return new AccessTypeCollection()
      .withData(source)
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(source.size()));
  }
}
