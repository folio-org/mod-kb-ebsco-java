package org.folio.rest.converter.facets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.folio.holdingsiq.model.Facets;
import org.folio.holdingsiq.model.SubjectFacet;
import org.junit.jupiter.api.Test;

class FacetsConverterTest {

  private final FacetsConverter converter = new FacetsConverter(new PackageFacetConverter(),
    new SubjectFacetConverter());

  @Test
  void shouldReturnOnlyMajorHeadingsWithOriginalCounts() {
    var facets = Facets.builder()
      .subjects(List.of(
        new SubjectFacet(1, null, "BISAC", "Science", 10),
        new SubjectFacet(2, 1, "BISAC", "Genetics", 4)))
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
  void shouldOrderByDescendingCountAndLimitTo1000Values() {
    var subjects = new ArrayList<SubjectFacet>();
    for (int i = 1; i <= 1100; i++) {
      subjects.add(new SubjectFacet(i, null, "BISAC", "Subject " + i, i));
    }

    var result = converter.convert(Facets.builder().subjects(subjects).build());

    var converted = result.getSubjects();
    assertEquals(1000, converted.size());
    assertEquals(1100, converted.getFirst().getCount());
    assertEquals(101, converted.getLast().getCount());
    assertEquals(converted, converted.stream()
      .sorted((a, b) -> b.getCount().compareTo(a.getCount()))
      .toList());
  }

  @Test
  void shouldReturnNullSubjectsAndPackagesWhenNotPresent() {
    var result = converter.convert(Facets.builder().build());

    assertNull(result.getSubjects());
    assertNull(result.getPackages());
  }
}
