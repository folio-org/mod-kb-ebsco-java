package org.folio.rest.model.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.holdingsiq.model.TitleSearchField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TitleSearchTest {

  @ParameterizedTest
  @CsvSource({
    "name,TITLE_NAME",
    "isxn,ISXN",
    "subject,SUBJECT",
    "publisher,PUBLISHER",
    "zdbid,ZDBID"
  })
  void shouldMapSupportedField(String field, TitleSearchField expected) {
    var search = TitleSearch.of("abc", field);

    assertThat(search.query()).isEqualTo("abc");
    assertThat(search.searchField()).isEqualTo(expected);
  }

  @Test
  void shouldDefaultToNameWhenFieldIsMissing() {
    var search = TitleSearch.of("abc", null);

    assertThat(search.query()).isEqualTo("abc");
    assertThat(search.searchField()).isEqualTo(TitleSearchField.TITLE_NAME);
  }

  @Test
  void shouldDefaultToNameWhenFieldIsUnknown() {
    var search = TitleSearch.of("abc", "bogus");

    assertThat(search.searchField()).isEqualTo(TitleSearchField.TITLE_NAME);
  }

  @Test
  void shouldHaveNoSearchWhenQueryIsMissing() {
    var search = TitleSearch.of(null, "isxn");

    assertThat(search.query()).isNull();
    assertThat(search.searchField()).isNull();
  }
}
