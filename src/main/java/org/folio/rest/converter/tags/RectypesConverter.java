package org.folio.rest.converter.tags;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.folio.repository.RecordType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class RectypesConverter implements Converter<List<String>, Set<org.folio.repository.RecordType>> {

  private final Converter<String, org.folio.repository.RecordType> recordTypeConverter;

  public RectypesConverter(Converter<String, RecordType> recordTypeConverter) {
    this.recordTypeConverter = recordTypeConverter;
  }

  @Override
  public Set<org.folio.repository.RecordType> convert(List<String> source) {
    return source.stream()
      .map(recordTypeConverter::convert)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }
}
