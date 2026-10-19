package org.folio.rest.converter.facets;

import static java.util.Objects.nonNull;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.folio.holdingsiq.model.Facets;
import org.folio.holdingsiq.model.SubjectFacet;
import org.folio.rest.jaxrs.model.PackageFacetsDto;
import org.folio.rest.jaxrs.model.SubjectFacetDto;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public final class PackageFacetsConverter implements Converter<Facets, PackageFacetsDto> {

  private final Converter<SubjectFacet, SubjectFacetDto> subjectFacetConverter;

  public PackageFacetsConverter(Converter<SubjectFacet, SubjectFacetDto> subjectFacetConverter) {
    this.subjectFacetConverter = subjectFacetConverter;
  }

  /**
   * Keeps only major headings (subjects without a parent) that have a positive count and orders them by
   * descending count. Counts are passed through as returned by HoldingsIQ.
   */
  @Override
  public PackageFacetsDto convert(Facets facets) {
    var subjects = nonNull(facets.getSubjects()) ? facets.getSubjects() : List.<SubjectFacet>of();
    var subjectDtos = subjects.stream()
      .filter(Objects::nonNull)
      .filter(subject -> subject.parentId() == null)
      .filter(subject -> subject.totalCount() != null && subject.totalCount() > 0)
      .sorted(Comparator.comparing(SubjectFacet::totalCount, Comparator.reverseOrder()))
      .map(subjectFacetConverter::convert)
      .toList();
    return new PackageFacetsDto().withSubjects(subjectDtos);
  }
}
