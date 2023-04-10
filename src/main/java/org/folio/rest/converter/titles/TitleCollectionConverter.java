package org.folio.rest.converter.titles;

import static java.util.Objects.isNull;
import static org.folio.common.ListUtils.mapItems;

import org.folio.holdingsiq.model.Facets;
import org.folio.holdingsiq.model.Titles;
import org.folio.rest.jaxrs.model.FacetsDto;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.Title;
import org.folio.rest.jaxrs.model.TitleCollection;
import org.folio.rest.jaxrs.model.TitleCollectionItem;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.TitleCollectionResult;
import org.folio.rmapi.result.TitleResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

public final class TitleCollectionConverter {

  private TitleCollectionConverter() {
  }

  @Component
  public static class FromTitles implements Converter<Titles, TitleCollection> {

    private final Converter<org.folio.holdingsiq.model.Title, TitleCollectionItem> titleConverter;

    private final Converter<Facets, FacetsDto> facetsConverter;

    @Autowired
    public FromTitles(Converter<org.folio.holdingsiq.model.Title, TitleCollectionItem> titleConverter,
                      Converter<Facets, FacetsDto> facetsConverter) {
      this.titleConverter = titleConverter;
      this.facetsConverter = facetsConverter;
    }

    @Override
    public TitleCollection convert(@NonNull Titles titles) {
      var titleList = mapItems(titles.getTitleList(), titleConverter::convert);
      var facetsDto = isNull(titles.getFacets()) ? null : facetsConverter.convert(titles.getFacets());

      return new TitleCollection()
        .withJsonapi(RestConstants.JSONAPI)
        .withMeta(new MetaTotalResults().withTotalResults(titles.getTotalResults()))
        .withData(titleList)
        .withFacets(facetsDto);
    }
  }

  @Component
  public static class FromTitleCollectionResult implements Converter<TitleCollectionResult, TitleCollection> {

    private final Converter<TitleResult, Title> titleConverter;
    private final Converter<Title, TitleCollectionItem> titlesConverter;
    private final Converter<Facets, FacetsDto> facetsConverter;

    public FromTitleCollectionResult(
      Converter<TitleResult, Title> titleConverter,
      Converter<Title, TitleCollectionItem> titlesConverter,
      Converter<Facets, FacetsDto> facetsConverter) {
      this.titleConverter = titleConverter;
      this.titlesConverter = titlesConverter;
      this.facetsConverter = facetsConverter;
    }

    @Override
    public TitleCollection convert(TitleCollectionResult source) {
      var titleList = mapItems(source.getTitleResults(), titleConverter::convert);
      var facetsDto = isNull(source.getFacets()) ? null : facetsConverter.convert(source.getFacets());
      return new TitleCollection()
        .withJsonapi(RestConstants.JSONAPI)
        .withMeta(new MetaTotalResults().withTotalResults(source.getTotalResults()))
        .withData(mapItems(titleList, titlesConverter::convert))
        .withFacets(facetsDto);
    }
  }
}
