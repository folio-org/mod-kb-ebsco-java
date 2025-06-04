package org.folio.rest.converter.common.attr;

import java.util.Map;
import java.util.Objects;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.folio.holdingsiq.model.EmbargoPeriod;
import org.folio.rest.jaxrs.model.EmbargoPeriod.EmbargoUnit;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class EmbargoPeriodConverter implements Converter<EmbargoPeriod, org.folio.rest.jaxrs.model.EmbargoPeriod> {

  private static final Map<String, EmbargoUnit> EMBARGO_UNITS;

  static {
    EMBARGO_UNITS = new CaseInsensitiveMap<>(4);
    EMBARGO_UNITS.put("Days", EmbargoUnit.DAYS);
    EMBARGO_UNITS.put("Weeks", EmbargoUnit.WEEKS);
    EMBARGO_UNITS.put("Months", EmbargoUnit.MONTHS);
    EMBARGO_UNITS.put("Years", EmbargoUnit.YEARS);
  }

  @Override
  public org.folio.rest.jaxrs.model.EmbargoPeriod convert(@Nullable EmbargoPeriod customEmbargoPeriod) {
    if (Objects.isNull(customEmbargoPeriod)) {
      return null;
    }
    org.folio.rest.jaxrs.model.EmbargoPeriod customEmbargo = new org.folio.rest.jaxrs.model.EmbargoPeriod();
    customEmbargo.setEmbargoUnit(EMBARGO_UNITS.get(customEmbargoPeriod.getEmbargoUnit()));
    customEmbargo.setEmbargoValue(customEmbargoPeriod.getEmbargoValue());
    return customEmbargo;
  }
}
