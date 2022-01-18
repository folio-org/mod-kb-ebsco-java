package org.folio.rest.converter.titles;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.folio.holdingsiq.model.AlternateTitle;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AlternateTitleConvertor implements Converter
  <List<AlternateTitle>, List<org.folio.rest.jaxrs.model.AlternateTitle>> {

  @Override
  public List<org.folio.rest.jaxrs.model.AlternateTitle> convert(List<AlternateTitle> alternateTitles) {
    if (Objects.isNull(alternateTitles)) {
      return Collections.emptyList();
    }

    return alternateTitles.stream()
      .map(alternateTitle -> new org.folio.rest.jaxrs.model.AlternateTitle()
        .withAlternateTitle(alternateTitle.getAlternateTitle())
        .withTitleType(alternateTitle.getTitleType()))
      .collect(Collectors.toList());
  }
}
