package org.folio.rest.converter.common.attr;

import org.folio.rest.jaxrs.model.Visibility;

/**
 * Maps the per-category visibility of HoldingsIQ to the eHoldings API visibility of packages and resources.
 */
public final class VisibilityDetailsMapper {

  private VisibilityDetailsMapper() {
  }

  public static Visibility toApiVisibility(org.folio.holdingsiq.model.Visibility visibility) {
    return new Visibility()
      .withCategory(Visibility.Category.fromValue(visibility.category().toUpperCase()))
      .withHidden(visibility.hidden())
      .withReason(visibility.reason());
  }
}
