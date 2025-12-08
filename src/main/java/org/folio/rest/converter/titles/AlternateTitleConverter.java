package org.folio.rest.converter.titles;

import java.util.List;
import org.folio.rest.jaxrs.model.AlternateTitle;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AlternateTitleConverter
  implements Converter<List<org.folio.holdingsiq.model.AlternateTitle>, List<AlternateTitle>> {

  @Override
  public List<AlternateTitle> convert(List<org.folio.holdingsiq.model.AlternateTitle> source) {
    return source.stream().map(this::convert).toList();
  }

  private AlternateTitle convert(org.folio.holdingsiq.model.AlternateTitle source) {
    return new AlternateTitle()
      .withAlternateTitle(source.getAlternateTitle())
      .withTitleType(source.getTitleType());
  }
}
