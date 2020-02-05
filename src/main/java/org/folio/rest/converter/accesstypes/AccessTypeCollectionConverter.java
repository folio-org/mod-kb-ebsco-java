package org.folio.rest.converter.accesstypes;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.AccessTypeCollection;
import org.folio.rest.jaxrs.model.AccessTypeCollectionItem;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.util.RestConstants;

@Component
public class AccessTypeCollectionConverter implements Converter<List<AccessTypeCollectionItem>, AccessTypeCollection> {

  @Override
  public AccessTypeCollection convert(@NotNull List<AccessTypeCollectionItem> source) {
    return new AccessTypeCollection()
      .withData(source)
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(source.size()));
  }
}
