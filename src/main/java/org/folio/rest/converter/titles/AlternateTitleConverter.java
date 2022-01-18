package org.folio.rest.converter.titles;

import org.folio.rest.jaxrs.model.AlternateTitle;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AlternateTitleConverter implements Converter<List<org.folio.holdingsiq.model.AlternateTitle>, List<AlternateTitle>> {
  @Override
  public List<AlternateTitle> convert(List<org.folio.holdingsiq.model.AlternateTitle> source) {
    return source == null ? null : source.stream().map(this::convert).collect(Collectors.toList());
  }

  private AlternateTitle convert(org.folio.holdingsiq.model.AlternateTitle source) {
    return new AlternateTitle()
      .withAlternateTitle(source.getAlternateTitle())
      .withTitleType(source.getTitleType());
  }

}
