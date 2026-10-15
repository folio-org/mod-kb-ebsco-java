package org.folio.rest.converter.facets;

import static org.folio.common.ListUtils.mapItemsNullable;

import java.util.Comparator;
import java.util.List;
import org.folio.holdingsiq.model.Facets;
import org.folio.holdingsiq.model.PackageFacet;
import org.folio.holdingsiq.model.SubjectFacet;
import org.folio.rest.jaxrs.model.FacetsDto;
import org.folio.rest.jaxrs.model.PackageFacetDto;
import org.folio.rest.jaxrs.model.SubjectFacetDto;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public final class FacetsConverter implements Converter<Facets, FacetsDto> {

  // Keep in sync with the default subjectfacetcount sent to HoldingsIQ by folio-holdingsiq-client
  static final int MAX_SUBJECT_FACETS = 1000;

  private final Converter<PackageFacet, PackageFacetDto> packageFacetConverter;
  private final Converter<SubjectFacet, SubjectFacetDto> subjectFacetConverter;

  public FacetsConverter(Converter<PackageFacet, PackageFacetDto> packageFacetConverter,
                         Converter<SubjectFacet, SubjectFacetDto> subjectFacetConverter) {
    this.packageFacetConverter = packageFacetConverter;
    this.subjectFacetConverter = subjectFacetConverter;
  }

  @Override
  public FacetsDto convert(Facets facets) {
    var packages = mapItemsNullable(facets.getPackages(), packageFacetConverter::convert);
    return new FacetsDto()
      .withPackages(packages)
      .withSubjects(convertSubjects(facets.getSubjects()));
  }

  /**
   * Keeps only major headings (subjects without a parent), orders them by descending count and caps the result.
   * Counts are passed through as returned by HoldingsIQ.
   */
  private @Nullable List<SubjectFacetDto> convertSubjects(@Nullable List<SubjectFacet> subjects) {
    if (subjects == null) {
      return null;
    }
    return subjects.stream()
      .filter(subject -> subject.parentId() == null)
      .sorted(Comparator.comparingInt(FacetsConverter::countOf).reversed())
      .limit(MAX_SUBJECT_FACETS)
      .map(subjectFacetConverter::convert)
      .toList();
  }

  private static int countOf(SubjectFacet subject) {
    return subject.totalCount() == null ? 0 : subject.totalCount();
  }
}
