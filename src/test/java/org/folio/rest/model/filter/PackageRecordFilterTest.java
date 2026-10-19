package org.folio.rest.model.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.folio.properties.common.SearchProperties;
import org.folio.rest.exception.QueryParamsValidationException;
import org.folio.rest.model.filter.PackageRecordFilter.PackageRecordFilterBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PackageRecordFilterTest {

  private static final SearchProperties SEARCH_PROPERTIES = new SearchProperties("contains", "contains", "mark");

  @Test
  void shouldIncludeSubjectFacetWhenRequestedRegardlessOfFilters() {
    var clientFilter = builder().build().toClientFilter(SEARCH_PROPERTIES);

    assertTrue(clientFilter.includeSubjectFacet());
  }

  @Test
  void shouldNotIncludeSubjectFacetWhenNotRequested() {
    var clientFilter = builder().query("foo").includeSubjectFacet(false).build().toClientFilter(SEARCH_PROPERTIES);

    assertFalse(clientFilter.includeSubjectFacet());
  }

  @Test
  void shouldConvertFilterSubjectToIntegerIds() {
    var clientFilter = builder().filterSubject(List.of("1", "22")).build().toClientFilter(SEARCH_PROPERTIES);

    assertEquals(List.of(1, 22), clientFilter.filterSubjectIds());
  }

  @Test
  void shouldPassNullSubjectIdsToClientFilterWhenNotProvided() {
    var clientFilter = builder().build().toClientFilter(SEARCH_PROPERTIES);

    assertNull(clientFilter.filterSubjectIds());
  }

  @Test
  void shouldPassSubjectFacetCountToClientFilter() {
    var clientFilter = builder().subjectFacetCount(50).build().toClientFilter(SEARCH_PROPERTIES);

    assertEquals(50, clientFilter.subjectFacetCount());
  }

  @Test
  void shouldPassNullSubjectFacetCountToClientFilterWhenNotProvided() {
    var clientFilter = builder().build().toClientFilter(SEARCH_PROPERTIES);

    assertNull(clientFilter.subjectFacetCount());
  }

  @Test
  void shouldBeTagsFilterWhenOnlyTagsAreProvided() {
    assertTrue(builder().filterTags(List.of("tag")).build().isTagsFilter());
  }

  @Test
  void shouldNotBeTagsFilterWhenFilterSubjectIsProvidedWithTags() {
    var filter = builder().filterTags(List.of("tag")).filterSubject(List.of("1")).build();

    assertFalse(filter.isTagsFilter());
  }

  @Test
  void shouldNotBeAccessTypeFilterWhenFilterSubjectIsProvidedWithAccessType() {
    var filter = builder().filterAccessType(List.of("type")).filterSubject(List.of("1")).build();

    assertFalse(filter.isAccessTypeFilter());
  }

  @Test
  void shouldValidateFilterSubjectWhenProvidedWithTags() {
    var builder = builder().filterTags(List.of("tag")).filterSubject(List.of("abc"));

    assertThrows(QueryParamsValidationException.class, builder::build);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidFilterSubjectCases")
  void shouldFailValidationWhenFilterSubjectIsNotNumeric(String name, List<String> filterSubject) {
    var builder = builder().filterSubject(filterSubject);

    var exception = assertThrows(QueryParamsValidationException.class, builder::build);

    assertTrue(exception.getMessageDetail()
      .contains(PackageFilterValidator.INVALID_FILTER_SUBJECT_PARAMETER_MESSAGE));
  }

  static List<Object[]> invalidFilterSubjectCases() {
    return List.of(
      new Object[] {"non-numeric", List.of("1", "abc")},
      new Object[] {"empty", List.of("")},
      new Object[] {"negative", List.of("-1")},
      new Object[] {"too large for integer", List.of("12345678901")},
      new Object[] {"null element", Arrays.asList("1", null)}
    );
  }

  private static PackageRecordFilterBuilder builder() {
    return PackageRecordFilter.builder().sort("relevance").page(1).count(25).includeSubjectFacet(true);
  }
}
