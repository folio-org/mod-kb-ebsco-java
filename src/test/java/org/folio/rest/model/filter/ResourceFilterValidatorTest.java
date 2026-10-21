package org.folio.rest.model.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.rest.model.filter.ResourceFilterValidator.INVALID_FILTER_ACCESS_PARAMETER_MESSAGE;
import static org.folio.rest.model.filter.ResourceFilterValidator.INVALID_FILTER_SELECTED_PARAMETER_MESSAGE;
import static org.folio.rest.model.filter.ResourceFilterValidator.INVALID_FILTER_TYPE_PARAMETER_MESSAGE;
import static org.folio.rest.model.filter.ResourceFilterValidator.INVALID_PACKAGE_ID_PARAMETER_MESSAGE;
import static org.folio.rest.model.filter.ResourceFilterValidator.INVALID_QUERY_FIELD_PARAMETER_MESSAGE;
import static org.folio.rest.model.filter.ResourceFilterValidator.INVALID_QUERY_PARAMETER_MESSAGE;
import static org.folio.rest.model.filter.ResourceFilterValidator.INVALID_SORT_PARAMETER_MESSAGE;

import java.util.List;
import org.folio.rest.exception.QueryParamsValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ResourceFilterValidatorTest {

  @Test
  void shouldAcceptValidFilter() {
    var builder = validFilter()
      .query("Mind")
      .queryField("name")
      .filterSelected("true")
      .filterType("book")
      .filterAccess("controlled");

    assertThatCode(builder::build).doesNotThrowAnyException();
  }

  @Test
  void shouldAcceptMissingQuery() {
    assertThatCode(validFilter()::build).doesNotThrowAnyException();
  }

  @Test
  void shouldAcceptMissingPackageId() {
    var builder = validFilter().packageId(null);

    assertThatCode(builder::build).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(strings = {"name", "isxn", "subject", "publisher", "zdbid"})
  void shouldAcceptSupportedQueryField(String queryField) {
    var builder = validFilter().query("Mind").queryField(queryField);

    assertThatCode(builder::build).doesNotThrowAnyException();
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

  @ParameterizedTest
  @ValueSource(strings = {"abc", "19", "19-abc"})
  void shouldRejectInvalidPackageId(String packageId) {
    assertValidationFails(validFilter().packageId(packageId), INVALID_PACKAGE_ID_PARAMETER_MESSAGE);
  }

  @Test
  void shouldReportAllFailures() {
    var builder = validFilter().query("").filterType("film").packageId("abc");

    assertThatThrownBy(builder::build)
      .isInstanceOfSatisfying(QueryParamsValidationException.class, e ->
        assertThat(e.getMessageDetail()).containsExactly(
          INVALID_QUERY_PARAMETER_MESSAGE, INVALID_FILTER_TYPE_PARAMETER_MESSAGE,
          INVALID_PACKAGE_ID_PARAMETER_MESSAGE));
  }

  @Test
  void shouldSkipValidationForTagSearch() {
    var builder = ResourceFilter.builder().sort("date").packageId("abc").filterTags(List.of("tag"));

    assertThatCode(builder::build).doesNotThrowAnyException();
  }

  @Test
  void shouldSkipValidationForAccessTypeSearch() {
    var builder = ResourceFilter.builder().sort("date").packageId("abc").filterAccessType(List.of("access type"));

    assertThatCode(builder::build).doesNotThrowAnyException();
  }

  private static ResourceFilter.ResourceFilterBuilder validFilter() {
    return ResourceFilter.builder().sort("name").packageId("19-3964");
  }

  private static void assertValidationFails(ResourceFilter.ResourceFilterBuilder builder, String expectedMessage) {
    assertThatThrownBy(builder::build)
      .isInstanceOfSatisfying(QueryParamsValidationException.class, e ->
        assertThat(e.getMessageDetail()).containsExactly(expectedMessage));
  }
}
