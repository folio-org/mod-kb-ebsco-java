package org.folio.rest.converter.tags;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class RectypesConverter implements Converter<List<String>, Set<org.folio.repository.RecordType>> {

  @Autowired
  private Converter<String, org.folio.repository.RecordType> recordTypeConverter;

  @Override
  public Set<org.folio.repository.RecordType> convert(@NonNull List<String> source) {
    return source.stream()
      .map(recType -> recordTypeConverter.convert(recType))
      .collect(Collectors.toSet());
  }

}
