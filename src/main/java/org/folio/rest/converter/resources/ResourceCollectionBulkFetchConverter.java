package org.folio.rest.converter.resources;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import org.folio.holdingsiq.model.Resource;
import org.folio.rest.jaxrs.model.FailedResourceIds;
import org.folio.rest.jaxrs.model.FailedResourcesInformation;
import org.folio.rest.jaxrs.model.ResourceBulkFetchCollection;
import org.folio.rest.jaxrs.model.ResourceBulkFetchCollectionItem;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.ResourceBulkResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ResourceCollectionBulkFetchConverter
  implements Converter<ResourceBulkResult, ResourceBulkFetchCollection> {

  private final Converter<Resource, ResourceBulkFetchCollectionItem> converter;

  public ResourceCollectionBulkFetchConverter(Converter<Resource, ResourceBulkFetchCollectionItem> converter) {
    this.converter = converter;
  }

  @Override
  public ResourceBulkFetchCollection convert(ResourceBulkResult resourceBulkResult) {
    List<ResourceBulkFetchCollectionItem> items = mapItems(resourceBulkResult.resources(), converter::convert);

    return new ResourceBulkFetchCollection()
      .withIncluded(items)
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new FailedResourcesInformation()
        .withFailed(new FailedResourceIds()
          .withResources(resourceBulkResult.failedResources()))
      );
  }
}
