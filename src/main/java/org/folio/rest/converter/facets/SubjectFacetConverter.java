package org.folio.rest.converter.facets;

import org.folio.holdingsiq.model.SubjectFacet;
import org.folio.rest.jaxrs.model.SubjectFacetDto;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public final class SubjectFacetConverter implements Converter<SubjectFacet, SubjectFacetDto> {

  @Override
  public SubjectFacetDto convert(SubjectFacet subjectFacet) {
    return new SubjectFacetDto()
      .withId(subjectFacet.id())
      .withName(subjectFacet.subjectName())
      .withSchema(subjectFacet.subjectSchema())
      .withCount(subjectFacet.totalCount());
  }
}
