package org.folio.rest.converter.resources;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.ResourceCollection;
import org.folio.rest.jaxrs.model.ResourceCollectionItem;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.TitleCollectionResult;
import org.folio.rmapi.result.TitleResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

public final class ResourceCollectionConverters {

  private ResourceCollectionConverters() {
  }

  @Component
  public static class FromTitles implements Converter<Titles, ResourceCollection> {

    private final Converter<Title, ResourceCollectionItem> converter;

    public FromTitles(Converter<Title, ResourceCollectionItem> converter) {
      this.converter = converter;
    }

    @Override
    public ResourceCollection convert(@NonNull Titles titles) {
      List<ResourceCollectionItem> titleList = mapItems(titles.getTitleList(), converter::convert);

      return new ResourceCollection()
        .withJsonapi(RestConstants.JSONAPI)
        .withMeta(new MetaTotalResults().withTotalResults(titles.getTotalResults()))
        .withData(titleList);
    }

  }

  @Component
  public static class FromTitleCollectionResult implements Converter<TitleCollectionResult, ResourceCollection> {

    private final Converter<TitleResult, ResourceCollectionItem> converter;

    public FromTitleCollectionResult(Converter<TitleResult, ResourceCollectionItem> converter) {
      this.converter = converter;
    }

    @Override
    public ResourceCollection convert(@NonNull TitleCollectionResult col) {
      List<ResourceCollectionItem> titleList = mapItems(col.getTitleResults(), converter::convert);

      return new ResourceCollection()
        .withJsonapi(RestConstants.JSONAPI)
        .withMeta(new MetaTotalResults().withTotalResults(col.getTotalResults()))
        .withData(titleList);

    }
  }
}
