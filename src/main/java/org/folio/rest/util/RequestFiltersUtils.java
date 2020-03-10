package org.folio.rest.util;

import static java.util.Collections.singletonList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;

public final class RequestFiltersUtils {

  private static final Pattern SPLIT_BY_COMMA_PATTERN = Pattern.compile("\\s*,\\s*");

  private RequestFiltersUtils() {
  }

  public static boolean isAccessTypeSearch(List<String> filterAccessTypes, String... otherFilters) {
    return isOnlySelectedFilterSearch(filterAccessTypes, otherFilters);
  }

  public static boolean isTagsSearch(String filterTags, String... otherFilters) {
    return isOnlySelectedFilterSearch(singletonList(filterTags), otherFilters);
  }

  public static boolean isOnlySelectedFilterSearch(List<String> selectedFilters, String... otherFilters) {
    return !selectedFilters.isEmpty()
      && IterableUtils.matchesAll(selectedFilters, StringUtils::isNotBlank)
      && IterableUtils.matchesAll(Arrays.asList(otherFilters), StringUtils::isBlank);
  }

  public static List<String> parseByComma(String string) {
    return StringUtils.isBlank(string)
      ? Collections.emptyList()
      : Arrays.asList(SPLIT_BY_COMMA_PATTERN.split(string));
  }
}
