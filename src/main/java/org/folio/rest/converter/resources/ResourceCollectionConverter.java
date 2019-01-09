package org.folio.rest.converter.resources;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.model.Titles;

@Component
public class ResourceCollectionConverter implements Converter<Titles, ResourceCollection> {

  @Autowired
  private Converter<Title, ResourceCollectionItem> converter;

  @Override
  public ResourceCollection convert(@NonNull Titles titles) {
    List<ResourceCollectionItem> titleList = titles.getTitleList().stream()
      .map(converter::convert)
      .collect(Collectors.toList());
    return new ResourceCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(titles.getTotalResults()))
      .withData(titleList);

  }
}
