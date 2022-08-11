package org.folio.rest.converter.titles;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import org.folio.holdingsiq.model.Titles;
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

    @Autowired
    private Converter<org.folio.holdingsiq.model.Title, TitleCollectionItem> titleConverter;

    @Override
    public TitleCollection convert(@NonNull Titles titles) {
      List<TitleCollectionItem> titleList = mapItems(titles.getTitleList(), titleConverter::convert);

      return new TitleCollection()
        .withJsonapi(RestConstants.JSONAPI)
        .withMeta(new MetaTotalResults().withTotalResults(titles.getTotalResults()))
        .withData(titleList);
    }
  }

  @Component
  public static class FromTitleCollectionResult implements Converter<TitleCollectionResult, TitleCollection> {

    private final Converter<TitleResult, Title> titleConverter;
    private final Converter<Title, TitleCollectionItem> titlesConverter;

    public FromTitleCollectionResult(
      Converter<TitleResult, Title> titleConverter,
      Converter<Title, TitleCollectionItem> titlesConverter) {
      this.titleConverter = titleConverter;
      this.titlesConverter = titlesConverter;
    }

    @Override
    public TitleCollection convert(TitleCollectionResult source) {
      List<Title> titleList = mapItems(source.getTitleResults(), titleConverter::convert);
      return new TitleCollection()
        .withJsonapi(RestConstants.JSONAPI)
        .withMeta(new MetaTotalResults().withTotalResults(source.getTotalResults()))
        .withData(mapItems(titleList, titlesConverter::convert));
    }
  }
}
