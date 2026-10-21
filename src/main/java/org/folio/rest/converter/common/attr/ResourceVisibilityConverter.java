package org.folio.rest.converter.common.attr;

import java.util.List;
import org.folio.holdingsiq.model.Visibility;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts the per-category visibility of HoldingsIQ to the single visibility flag of the eHoldings API:
 * the resource is hidden when it is hidden in at least one category, and visible when it is visible in all of them.
 */
@Component
public class ResourceVisibilityConverter implements Converter<List<Visibility>, VisibilityData> {

  private static final String HIDDEN_BY_EP_REASON = "Hidden by EP";
  private static final String SET_BY_SYSTEM_REASON = "Set by system";

  @Override
  public VisibilityData convert(List<Visibility> visibilityDetails) {
    var hiddenCategory = visibilityDetails.stream()
      .filter(Visibility::hidden)
      .findFirst();
    return new VisibilityData()
      .withIsHidden(hiddenCategory.isPresent())
      .withReason(hiddenCategory
        .map(visibility -> HIDDEN_BY_EP_REASON.equals(visibility.reason()) ? SET_BY_SYSTEM_REASON : "")
        .orElse(""));
  }
}
