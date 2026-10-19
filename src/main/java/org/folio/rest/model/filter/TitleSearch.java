package org.folio.rest.model.filter;

import java.util.Set;
import org.folio.holdingsiq.model.TitleSearchField;
import org.jspecify.annotations.Nullable;

/**
 * Search term and field resolved from the {@code q} and {@code q[field]} parameters; the field is one of
 * {@code name}, {@code isxn}, {@code subject}, {@code publisher} or {@code zdbid}; a missing or unknown field falls
 * back to {@code name}.
 */
record TitleSearch(@Nullable String query, @Nullable TitleSearchField searchField) {

  static final Set<String> FIELDS = Set.of("name", "isxn", "subject", "publisher", "zdbid");

  static TitleSearch of(@Nullable String query, @Nullable String field) {
    if (query == null) {
      return new TitleSearch(null, null);
    }
    return new TitleSearch(query, resolveSearchField(field));
  }

  private static TitleSearchField resolveSearchField(@Nullable String field) {
    if (field == null) {
      return TitleSearchField.TITLE_NAME;
    }
    return switch (field) {
      case "isxn" -> TitleSearchField.ISXN;
      case "subject" -> TitleSearchField.SUBJECT;
      case "publisher" -> TitleSearchField.PUBLISHER;
      case "zdbid" -> TitleSearchField.ZDBID;
      default -> TitleSearchField.TITLE_NAME;
    };
  }
}
