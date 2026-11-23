package org.folio.rest.converter.common.attr;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.jaxrs.model.Visibility;
import org.jspecify.annotations.Nullable;

/**
 * Maps the per-category visibility between HoldingsIQ and the eHoldings API for packages and resources.
 */
public final class VisibilityDetailsMapper {

  public static final String HIDDEN_BY_CUSTOMER = "Hidden by Customer";

  private VisibilityDetailsMapper() {
  }

  public static Visibility toApiVisibility(org.folio.holdingsiq.model.Visibility visibility) {
    return new Visibility()
      .withCategory(Visibility.Category.fromValue(visibility.category().toUpperCase()))
      .withHidden(visibility.hidden())
      .withReason(visibility.reason());
  }

  /**
   * Maps the visibility of a PUT request to HoldingsIQ. The reason sent by the client is ignored: it is always
   * {@link #HIDDEN_BY_CUSTOMER} for hidden categories and empty otherwise.
   */
  public static org.folio.holdingsiq.model.Visibility toRmApiVisibility(Visibility visibility) {
    var reason = Boolean.TRUE.equals(visibility.getHidden()) ? HIDDEN_BY_CUSTOMER : null;
    return new org.folio.holdingsiq.model.Visibility(visibility.getCategory().value(), visibility.getHidden(), reason);
  }

  /**
   * Maps the visibility of a PUT request to HoldingsIQ, or returns {@code null} when the request does not set
   * any, so that HoldingsIQ keeps the current visibility.
   */
  public static @Nullable List<org.folio.holdingsiq.model.Visibility> toRmApiVisibilityDetails(
    @Nullable List<Visibility> visibility) {
    return CollectionUtils.isEmpty(visibility)
           ? null
           : mapItems(visibility, VisibilityDetailsMapper::toRmApiVisibility);
  }
}
