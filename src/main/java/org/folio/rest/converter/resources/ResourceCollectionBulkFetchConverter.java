package org.folio.rest.converter.resources;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import org.folio.holdingsiq.model.Title;
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

  private final Converter<Title, ResourceBulkFetchCollectionItem> converter;

  public ResourceCollectionBulkFetchConverter(Converter<Title, ResourceBulkFetchCollectionItem> converter) {
    this.converter = converter;
  }

  @Override
  public ResourceBulkFetchCollection convert(ResourceBulkResult resourceBulkResult) {
    List<ResourceBulkFetchCollectionItem> titleList = mapItems(resourceBulkResult.titles().getTitleList(),
      converter::convert);

    return new ResourceBulkFetchCollection()
      .withIncluded(titleList)
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new FailedResourcesInformation()
        .withFailed(new FailedResourceIds()
          .withResources(resourceBulkResult.failedResources()))
      );
  }
}
