package org.folio.rest.converter.resources;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.List;
import org.folio.holdingsiq.model.Visibility;
import org.jspecify.annotations.Nullable;

/**
 * Maps the visibility of the eHoldings API to the per-category visibility of HoldingsIQ v2. A per-category list is
 * sent as is; the single visibility flag is written to the PF, FTF and MARC categories.
 */
public final class ResourceVisibility {

  static final String HIDDEN_BY_CUSTOMER = "Hidden by Customer";

  private static final List<String> CATEGORIES = List.of("PF", "FTF", "MARC");

  private ResourceVisibility() {
  }

  /**
   * Returns {@code null} when the request sets no visibility, so that HoldingsIQ keeps the current one.
   */
  public static @Nullable List<Visibility> toVisibilityDetails(
    @Nullable List<org.folio.rest.jaxrs.model.Visibility> visibility, @Nullable Boolean hidden) {
    if (isNotEmpty(visibility)) {
      return visibility.stream()
        .map(item -> toVisibility(item.getCategory().value(), item.getHidden()))
        .toList();
    }
    if (hidden == null) {
      return null;
    }
    return CATEGORIES.stream()
      .map(category -> toVisibility(category, hidden))
      .toList();
  }

  // the reason is only sent for a hidden category
  private static Visibility toVisibility(String category, boolean hidden) {
    return Visibility.builder().category(category).hidden(hidden).reason(hidden ? HIDDEN_BY_CUSTOMER : null).build();
  }
}
