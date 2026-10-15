package org.folio.rest.model.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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

  static List<Object[]> facetRequestCases() {
    return List.of(
      new Object[] {"no query or filters", builder(), false},
      new Object[] {"filterType=all", builder().filterType("all"), false},
      new Object[] {"query", builder().query("foo"), true},
      new Object[] {"filterType=ebook", builder().filterType("ebook"), true},
      new Object[] {"filterSelected=true", builder().filterSelected("true"), true},
      new Object[] {"filterFreeAccess=public", builder().filterFreeAccess("public"), true},
      new Object[] {"filterSubject", builder().filterSubject(List.of("1")), true},
      new Object[] {"includeSubjectFacet=false with query",
        builder().query("foo").includeSubjectFacet(false), false}
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("facetRequestCases")
  void shouldResolveIncludeSubjectFacet(String name, PackageRecordFilterBuilder builder, boolean expected) {
    var clientFilter = builder.build().toClientFilter(SEARCH_PROPERTIES);

    assertEquals(expected, clientFilter.includeSubjectFacet());
  }

  @Test
  void shouldConvertFilterSubjectToIntegerIds() {
    var filter = builder().filterSubject(List.of("1", "22")).build();

    assertEquals(List.of(1, 22), filter.toClientFilter(SEARCH_PROPERTIES).filterSubjectIds());
  }

  @Test
  void shouldReturnNullSubjectIdsWhenFilterSubjectIsNullOrEmpty() {
    assertNull(builder().build().toClientFilter(SEARCH_PROPERTIES).filterSubjectIds());
    assertNull(builder().filterSubject(new ArrayList<>()).build().toClientFilter(SEARCH_PROPERTIES)
      .filterSubjectIds());
  }

  @Test
  void shouldFailValidationWhenFilterSubjectIsNotNumeric() {
    var builder = builder().filterSubject(Arrays.asList("1", "abc"));

    var exception = assertThrows(QueryParamsValidationException.class, builder::build);

    assertTrue(exception.getMessageDetail()
      .contains(PackageFilterValidator.INVALID_FILTER_SUBJECT_PARAMETER_MESSAGE));
  }

  @Test
  void shouldPassValidationWhenFilterSubjectIsNumeric() {
    var filter = builder().filterSubject(List.of("26224")).build();

    assertFalse(filter.getFilterSubject().isEmpty());
  }

  private static PackageRecordFilterBuilder builder() {
    return PackageRecordFilter.builder().sort("relevance").page(1).count(25).includeSubjectFacet(true);
  }
}
