package org.folio.rest.converter.titles;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.TitleCollection;
import org.folio.rest.util.RestConstants;

@Component
public class TitleCollectionConverter implements Converter<Titles, TitleCollection> {

  @Autowired
  private Converter<Title, org.folio.rest.jaxrs.model.Titles> titleConverter;

  @Override
  public TitleCollection convert(@NonNull Titles titles) {
    List<org.folio.rest.jaxrs.model.Titles> titleList = mapItems(titles.getTitleList(), titleConverter::convert);

    return new TitleCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(titles.getTotalResults()))
      .withData(titleList);
  }
}
