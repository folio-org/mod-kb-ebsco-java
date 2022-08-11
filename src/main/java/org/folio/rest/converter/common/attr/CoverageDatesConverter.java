package org.folio.rest.converter.common.attr;

import static org.folio.common.ListUtils.mapItems;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.rest.jaxrs.model.Coverage;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class CoverageDatesConverter implements Converter<List<CoverageDates>, List<Coverage>> {

  @Override
  public List<Coverage> convert(@Nullable List<CoverageDates> coverageList) {
    if (Objects.isNull(coverageList)) {
      return Collections.emptyList();
    }
    return mapItems(coverageList,
      coverageItem -> new Coverage()
        .withBeginCoverage(coverageItem.getBeginCoverage())
        .withEndCoverage(coverageItem.getEndCoverage()));
  }

}
