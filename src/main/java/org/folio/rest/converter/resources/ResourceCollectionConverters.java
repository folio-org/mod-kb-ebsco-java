package org.folio.rest.converter.resources;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import org.folio.holdingsiq.model.Resource;
import org.folio.holdingsiq.model.Resources;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.ResourceItemCollectionResult;
import org.folio.rmapi.result.ResourceItemResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

public final class ResourceCollectionConverters {

  private ResourceCollectionConverters() {
  }

  @Component
  public static class FromResources implements Converter<Resources, ResourceCollection> {

    private final Converter<Resource, ResourceCollectionItem> converter;

    public FromResources(Converter<Resource, ResourceCollectionItem> converter) {
      this.converter = converter;
    }

    @Override
    public ResourceCollection convert(Resources resources) {
      List<ResourceCollectionItem> items = mapItems(resources.getTitles(), converter::convert);

      return new ResourceCollection()
        .withJsonapi(RestConstants.JSONAPI)
        .withMeta(new MetaTotalResults().withTotalResults(resources.getTotalResults()))
        .withData(items);
    }
  }

  @Component
  public static class FromResourceItemCollectionResult
    implements Converter<ResourceItemCollectionResult, ResourceCollection> {

    private final Converter<ResourceItemResult, ResourceCollectionItem> converter;

    public FromResourceItemCollectionResult(Converter<ResourceItemResult, ResourceCollectionItem> converter) {
      this.converter = converter;
    }

    @Override
    public ResourceCollection convert(ResourceItemCollectionResult col) {
      List<ResourceCollectionItem> items = mapItems(col.items(), converter::convert);

      return new ResourceCollection()
        .withJsonapi(RestConstants.JSONAPI)
        .withMeta(new MetaTotalResults().withTotalResults(col.totalResults()))
        .withData(items);
    }
  }
}
