package org.folio.rest.converter.titles;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.TitleCollection;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.Title;
import org.folio.rmapi.model.Titles;

@Component
public class TitleCollectionConverter implements Converter<Titles, TitleCollection> {

  @Autowired
  private Converter<Title, org.folio.rest.jaxrs.model.Titles> titleConverter;

  @Override
  public TitleCollection convert(@NonNull Titles titles) {
    List<org.folio.rest.jaxrs.model.Titles> titleList = titles.getTitleList().stream()
      .map(titleConverter::convert)
      .collect(Collectors.toList());
    return new TitleCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(titles.getTotalResults()))
      .withData(titleList);
  }
}
