package org.folio.rest.model.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.folio.holdingsiq.model.PackageFilterVisibility;
import org.folio.rest.exception.QueryParamsValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PackageFilterValidatorTest {

  @ParameterizedTest
  @CsvSource({
    "visibleInPF, VISIBLE_IN_PF",
    "visibleInFTF, VISIBLE_IN_FTF",
    "visibleInMARC, INCLUDED_IN_MARC",
    "visibleSomewhere, VISIBLE_SOMEWHERE",
    "hiddenInPF, HIDDEN_IN_PF",
    "hiddenInFTF, HIDDEN_IN_FTF",
    "hiddenInMARC, EXCLUDED_FROM_MARC",
    "hiddenSomewhere, HIDDEN_SOMEWHERE"
  })
  void shouldAcceptAndMapSupportedVisibilityFilter(String value, PackageFilterVisibility expected) {
    var filter = PackageRecordFilter.builder().sort("name").filterSelected("true").filterVisibility(value).build();

    assertThat(filter.getPackageFilterVisibility()).contains(expected);
  }

  @Test
  void shouldRejectUnknownVisibilityFilter() {
    var builder = PackageRecordFilter.builder().sort("name").filterVisibility("hiddenNowhere");

    assertThatThrownBy(builder::build).isInstanceOf(QueryParamsValidationException.class);
  }

  @Test
  void shouldAcceptMissingVisibilityFilter() {
    assertThatCode(() -> PackageRecordFilter.builder().sort("name").build()).doesNotThrowAnyException();
  }
}
