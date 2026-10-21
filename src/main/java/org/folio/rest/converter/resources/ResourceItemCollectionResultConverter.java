package org.folio.rest.converter.resources;

import static org.folio.common.ListUtils.mapItems;

import org.folio.holdingsiq.model.Resources;
import org.folio.rmapi.result.ResourceItemCollectionResult;
import org.folio.rmapi.result.ResourceItemResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ResourceItemCollectionResultConverter implements Converter<Resources, ResourceItemCollectionResult> {

  @Override
  public ResourceItemCollectionResult convert(Resources resources) {
    return new ResourceItemCollectionResult(mapItems(resources.getTitles(), ResourceItemResult::new),
      resources.getFacets(), resources.getTotalResults());
  }
}
