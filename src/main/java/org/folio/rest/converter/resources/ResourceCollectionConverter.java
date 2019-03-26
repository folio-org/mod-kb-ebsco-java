package org.folio.rest.converter.resources;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.util.RestConstants;

@Component
public class ResourceCollectionConverter implements Converter<Titles, ResourceCollection> {

  @Autowired
  private Converter<Title, ResourceCollectionItem> converter;

  @Override
  public ResourceCollection convert(@NonNull Titles titles) {
    List<ResourceCollectionItem> titleList = mapItems(titles.getTitleList(), converter::convert);
    
    return new ResourceCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(titles.getTotalResults()))
      .withData(titleList);

  }
}
