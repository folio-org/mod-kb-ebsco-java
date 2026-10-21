package org.folio.rest.model.filter;

import org.jspecify.annotations.Nullable;

/**
 * Maps the deprecated {@code filter[name|isxn|subject|publisher]} parameters onto {@code q} and {@code q[field]}.
 * An explicit {@code q} always wins. Kept until the UI switches to {@code q}.
 */
public record LegacySearchParams(@Nullable String query, @Nullable String queryField) {

  public static LegacySearchParams resolve(@Nullable String query, @Nullable String queryField,
                                           @Nullable String filterName, @Nullable String filterIsxn,
                                           @Nullable String filterSubject, @Nullable String filterPublisher) {
    if (query != null) {
      return new LegacySearchParams(query, queryField);
    }
    if (filterName != null) {
      return new LegacySearchParams(filterName, "name");
    }
    if (filterIsxn != null) {
      return new LegacySearchParams(filterIsxn, "isxn");
    }
    if (filterSubject != null) {
      return new LegacySearchParams(filterSubject, "subject");
    }
    if (filterPublisher != null) {
      return new LegacySearchParams(filterPublisher, "publisher");
    }
    return new LegacySearchParams(null, queryField);
  }
}
