package org.folio.rest.converter.facets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.folio.holdingsiq.model.Facets;
import org.folio.holdingsiq.model.SubjectFacet;
import org.folio.rest.jaxrs.model.SubjectFacetDto;
import org.junit.jupiter.api.Test;

class PackageFacetsConverterTest {

  private final PackageFacetsConverter converter = new PackageFacetsConverter(new SubjectFacetConverter());

  @Test
  void shouldMapSubjectWithOriginalCount() {
    var facets = Facets.builder()
      .subjects(List.of(new SubjectFacet(1, null, "BISAC", "Science", 10)))
      .build();

    var result = converter.convert(facets);

    assertEquals(1, result.getSubjects().size());
    var subject = result.getSubjects().getFirst();
    assertEquals(1, subject.getId());
    assertEquals("Science", subject.getName());
    assertEquals("BISAC", subject.getSchema());
    assertEquals(10, subject.getCount());
  }

  @Test
  void shouldReturnOnlyMajorHeadings() {
    var facets = Facets.builder()
      .subjects(List.of(
        new SubjectFacet(1, null, "BISAC", "Science", 10),
        new SubjectFacet(2, 1, "BISAC", "Genetics", 4)))
      .build();

    var result = converter.convert(facets);

    assertEquals(List.of(1), result.getSubjects().stream().map(SubjectFacetDto::getId).toList());
  }

  @Test
  void shouldSkipNullSubjectsAndSubjectsWithoutPositiveCount() {
    var facets = Facets.builder()
      .subjects(Arrays.asList(
        null,
        new SubjectFacet(1, null, "BISAC", "Science", 10),
        new SubjectFacet(2, null, "BISAC", "Zero count", 0),
        new SubjectFacet(3, null, "BISAC", "Null count", null)))
      .build();

    var result = converter.convert(facets);

    assertEquals(List.of(1), result.getSubjects().stream().map(SubjectFacetDto::getId).toList());
  }

  @Test
  void shouldOrderSubjectsByDescendingCount() {
    var facets = Facets.builder()
      .subjects(List.of(
        new SubjectFacet(1, null, "BISAC", "Science", 10),
        new SubjectFacet(2, null, "BISAC", "History", 20),
        new SubjectFacet(3, null, "BISAC", "Art", 15)))
      .build();

    var result = converter.convert(facets);

    assertEquals(List.of(20, 15, 10), result.getSubjects().stream().map(SubjectFacetDto::getCount).toList());
  }

  @Test
  void shouldReturnEmptySubjectsWhenNotPresent() {
    var result = converter.convert(Facets.builder().build());

    assertTrue(result.getSubjects().isEmpty());
  }
}
