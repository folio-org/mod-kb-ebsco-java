package org.folio.rest.converter.common.attr;

import static org.folio.common.ListUtils.mapItems;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.folio.holdingsiq.model.Subject;
import org.folio.rest.jaxrs.model.TitleSubject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class SubjectsConverter implements Converter<List<Subject>, List<TitleSubject>> {

  @Override
  public List<TitleSubject> convert(@Nullable List<Subject> subjectsList) {
    if (Objects.isNull(subjectsList)) {
      return Collections.emptyList();
    }
    return mapItems(subjectsList,
      subject -> new TitleSubject()
        .withSubject(subject.getValue())
        .withType(subject.getType()));
  }

}
