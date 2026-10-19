package org.folio.rest.model.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.rest.model.filter.TitleFilterValidator.INVALID_FILTER_ACCESS_PARAMETER_MESSAGE;
import static org.folio.rest.model.filter.TitleFilterValidator.INVALID_FILTER_PACKAGE_IDS_PARAMETER_MESSAGE;
import static org.folio.rest.model.filter.TitleFilterValidator.INVALID_FILTER_SELECTED_PARAMETER_MESSAGE;
import static org.folio.rest.model.filter.TitleFilterValidator.INVALID_FILTER_TYPE_PARAMETER_MESSAGE;
import static org.folio.rest.model.filter.TitleFilterValidator.INVALID_QUERY_FIELD_PARAMETER_MESSAGE;
import static org.folio.rest.model.filter.TitleFilterValidator.INVALID_QUERY_PARAMETER_MESSAGE;
import static org.folio.rest.model.filter.TitleFilterValidator.INVALID_QUERY_TYPE_PARAMETER_MESSAGE;
import static org.folio.rest.model.filter.TitleFilterValidator.INVALID_SORT_PARAMETER_MESSAGE;
import static org.folio.rest.model.filter.TitleFilterValidator.MISSING_QUERY_PARAMETER_MESSAGE;

import java.util.List;
import org.folio.rest.exception.QueryParamsValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TitleFilterValidatorTest {

  @Test
  void shouldAcceptValidFilter() {
    var builder = validFilter()
      .queryType("contains")
      .filterSelected("true")
      .filterType("book")
      .filterAccess("public")
      .filterPackageIds(List.of("1", "2"));

    assertThatCode(builder::build).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(strings = {"name", "isxn", "subject", "publisher", "zdbid"})
  void shouldAcceptSupportedQueryField(String queryField) {
    var builder = validFilter().queryField(queryField);

    assertThatCode(builder::build).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(strings = {"any", "contains", "exactmatch", "beginswith", "proximity", "exactphrase", "advanced"})
  void shouldAcceptSupportedQueryType(String queryType) {
    var builder = validFilter().queryType(queryType);

    assertThatCode(builder::build).doesNotThrowAnyException();
  }

  @Test
  void shouldRejectMissingQuery() {
    assertValidationFails(validFilter().query(null), MISSING_QUERY_PARAMETER_MESSAGE);
  }

  @Test
  void shouldRejectEmptyQuery() {
    assertValidationFails(validFilter().query(""), INVALID_QUERY_PARAMETER_MESSAGE);
  }

  @Test
  void shouldRejectBlankQuery() {
    assertValidationFails(validFilter().query("   "), INVALID_QUERY_PARAMETER_MESSAGE);
  }

  @Test
  void shouldRejectInvalidSort() {
    assertValidationFails(validFilter().sort("date"), INVALID_SORT_PARAMETER_MESSAGE);
  }

  @Test
  void shouldRejectInvalidQueryType() {
    assertValidationFails(validFilter().queryType("fuzzy"), INVALID_QUERY_TYPE_PARAMETER_MESSAGE);
  }

  @Test
  void shouldRejectInvalidQueryField() {
    assertValidationFails(validFilter().queryField("bogus"), INVALID_QUERY_FIELD_PARAMETER_MESSAGE);
  }

  @Test
  void shouldAcceptAllFilterType() {
    assertThatCode(validFilter().filterType("all")::build).doesNotThrowAnyException();
  }

  @Test
  void shouldRejectInvalidFilterType() {
    assertValidationFails(validFilter().filterType("film"), INVALID_FILTER_TYPE_PARAMETER_MESSAGE);
  }

  @Test
  void shouldRejectInvalidFilterSelected() {
    assertValidationFails(validFilter().filterSelected("maybe"), INVALID_FILTER_SELECTED_PARAMETER_MESSAGE);
  }

  @Test
  void shouldRejectInvalidFilterAccess() {
    assertValidationFails(validFilter().filterAccess("free"), INVALID_FILTER_ACCESS_PARAMETER_MESSAGE);
  }

  @Test
  void shouldRejectNonNumericFilterPackageIds() {
    assertValidationFails(validFilter().filterPackageIds(List.of("1", "abc")),
      INVALID_FILTER_PACKAGE_IDS_PARAMETER_MESSAGE);
  }

  @Test
  void shouldRejectCommaSeparatedFilterPackageIds() {
    assertValidationFails(validFilter().filterPackageIds(List.of("1,2")),
      INVALID_FILTER_PACKAGE_IDS_PARAMETER_MESSAGE);
  }

  @Test
  void shouldRejectEmptyFilterPackageId() {
    assertValidationFails(validFilter().filterPackageIds(List.of("")),
      INVALID_FILTER_PACKAGE_IDS_PARAMETER_MESSAGE);
  }

  @Test
  void shouldRejectFilterPackageIdOutOfIntegerRange() {
    assertValidationFails(validFilter().filterPackageIds(List.of("99999999999")),
      INVALID_FILTER_PACKAGE_IDS_PARAMETER_MESSAGE);
  }

  @Test
  void shouldReportAllFailures() {
    var builder = validFilter().query(null).filterType("film").filterAccess("free");

    assertThatThrownBy(builder::build)
      .isInstanceOfSatisfying(QueryParamsValidationException.class, e ->
        assertThat(e.getMessageDetail()).containsExactly(
          MISSING_QUERY_PARAMETER_MESSAGE, INVALID_FILTER_TYPE_PARAMETER_MESSAGE,
          INVALID_FILTER_ACCESS_PARAMETER_MESSAGE));
  }

  @Test
  void shouldSkipValidationForTagSearch() {
    var builder = TitleFilter.builder().sort("date").filterTags(List.of("tag"));

    assertThatCode(builder::build).doesNotThrowAnyException();
  }

  @Test
  void shouldSkipValidationForAccessTypeSearch() {
    var builder = TitleFilter.builder().sort("date").filterAccessType(List.of("access type"));

    assertThatCode(builder::build).doesNotThrowAnyException();
  }

  private static TitleFilter.TitleFilterBuilder validFilter() {
    return TitleFilter.builder().sort("name").query("Mind").queryField("name");
  }

  private static void assertValidationFails(TitleFilter.TitleFilterBuilder builder, String expectedMessage) {
    assertThatThrownBy(builder::build)
      .isInstanceOfSatisfying(QueryParamsValidationException.class, e ->
        assertThat(e.getMessageDetail()).containsExactly(expectedMessage));
  }
}
