package org.folio.rest.converter.common.attr;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rmapi.model.CoverageDates;

@Component
public class CoverageDatesConverter implements Converter<List<CoverageDates>, List<Coverage>> {

  @Override
  public List<Coverage> convert(@Nullable List<CoverageDates> coverageList) {
    if(Objects.isNull(coverageList)) {
      return Collections.emptyList();
    }
    return coverageList.stream().map(coverageItem ->
      new Coverage()
        .withBeginCoverage(coverageItem.getBeginCoverage())
        .withEndCoverage(coverageItem.getEndCoverage())
    )
      .collect(Collectors.toList());
  }

}
