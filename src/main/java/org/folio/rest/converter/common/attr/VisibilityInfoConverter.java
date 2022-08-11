package org.folio.rest.converter.common.attr;

import java.util.Objects;
import org.folio.holdingsiq.model.VisibilityInfo;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class VisibilityInfoConverter implements Converter<VisibilityInfo, VisibilityData> {

  @Override
  public VisibilityData convert(@Nullable VisibilityInfo visibilityData) {
    if (Objects.isNull(visibilityData)) {
      return null;
    }
    org.folio.rest.jaxrs.model.VisibilityData visibility = new org.folio.rest.jaxrs.model.VisibilityData();
    visibility.setIsHidden(visibilityData.getIsHidden());
    visibility.setReason(visibilityData.getReason().equals("Hidden by EP") ? "Set by system" : "");
    return visibility;
  }

}
