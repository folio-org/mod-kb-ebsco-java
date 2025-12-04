package org.folio.rest.converter.titles;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import org.folio.holdingsiq.model.Titles;
import org.folio.rmapi.result.TitleCollectionResult;
import org.folio.rmapi.result.TitleResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TitleCollectionResultConverter implements Converter<Titles, TitleCollectionResult> {

  @Override
  public TitleCollectionResult convert(Titles titles) {
    List<TitleResult> titleResults = mapItems(titles.getTitleList(), title -> new TitleResult(title, false));

    return TitleCollectionResult.builder()
      .titleResults(titleResults)
      .facets(titles.getFacets())
      .totalResults(titles.getTotalResults()).build();
  }
}
