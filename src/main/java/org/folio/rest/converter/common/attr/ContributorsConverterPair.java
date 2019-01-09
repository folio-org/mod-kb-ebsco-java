package org.folio.rest.converter.common.attr;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.Contributors;
import org.folio.rmapi.model.Contributor;

public class ContributorsConverterPair {

  private ContributorsConverterPair() {
  }

  @Component
  public static class FromRMApi implements Converter<List<Contributor>, List<Contributors>> {

    @Override
    public List<Contributors> convert(@Nullable List<Contributor> contributorList) {
      if (Objects.isNull(contributorList)) {
        return Collections.emptyList();
      }
      return contributorList.stream().map(contributor ->
        new Contributors()
          .withContributor(contributor.getTitleContributor())
          .withType(StringUtils.capitalize(contributor.getType()))
      )
        .collect(Collectors.toList());
    }
  }

  @Component
  public static class ToRMApi implements Converter<List<Contributors>, List<Contributor>> {

    @Override
    public List<Contributor> convert(@NonNull List<Contributors> contributorList) {
      return contributorList.stream().map(contributor ->
          org.folio.rmapi.model.Contributor.builder()
            .titleContributor(contributor.getContributor())
            .type(StringUtils.capitalize(contributor.getType())).build()
        )
        .collect(Collectors.toList());
    }
  }
  
}
