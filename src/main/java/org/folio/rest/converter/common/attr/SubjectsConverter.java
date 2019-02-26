package org.folio.rest.converter.common.attr;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Subject;
import org.folio.rest.jaxrs.model.TitleSubject;

@Component
public class SubjectsConverter implements Converter<List<Subject>, List<TitleSubject>> {

  @Override
  public List<TitleSubject> convert(@Nullable List<Subject> subjectsList) {
    if(Objects.isNull(subjectsList)) {
      return Collections.emptyList();
    }
    return subjectsList.stream().map(subject ->
      new TitleSubject()
        .withSubject(subject.getValue())
        .withType(subject.getType())
    )
      .collect(Collectors.toList());
  }

}
