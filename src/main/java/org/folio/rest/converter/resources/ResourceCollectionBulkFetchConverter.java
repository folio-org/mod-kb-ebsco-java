package org.folio.rest.converter.resources;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Title;
import org.folio.rest.jaxrs.model.MetaResourcesInformation;
import org.folio.rest.jaxrs.model.ResourceBulkFetchCollection;
import org.folio.rest.jaxrs.model.ResourceBulkFetchCollectionItem;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.ResourceBulkResult;

@Component
public class ResourceCollectionBulkFetchConverter implements Converter<ResourceBulkResult, ResourceBulkFetchCollection> {

  @Autowired
  private Converter<Title, ResourceBulkFetchCollectionItem> converter;

  @Override
  public ResourceBulkFetchCollection convert(ResourceBulkResult resourceBulkResult) {
    List<ResourceBulkFetchCollectionItem> titleList = mapItems(resourceBulkResult.getTitles(), converter::convert);
    return new ResourceBulkFetchCollection()
      .withIncluded(titleList)
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaResourcesInformation()
      .withFailed(new org.folio.rest.jaxrs.model.Failed()
        .withResources(resourceBulkResult.getFailedResources()))
    );
  }
}
