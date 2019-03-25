package org.folio.rest.converter.common.attr;

import static org.folio.common.ListUtils.mapItems;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.Contributor;
import org.folio.rest.jaxrs.model.Contributors;

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
      return mapItems(contributorList,
        contributor -> new Contributors()
            .withContributor(contributor.getTitleContributor())
            .withType(StringUtils.capitalize(contributor.getType())));
    }
  }

  @Component
  public static class ToRMApi implements Converter<List<Contributors>, List<Contributor>> {

    @Override
    public List<Contributor> convert(@NonNull List<Contributors> contributorList) {
      return mapItems(contributorList,
          contributor ->org.folio.holdingsiq.model.Contributor.builder()
            .titleContributor(contributor.getContributor())
            .type(StringUtils.capitalize(contributor.getType())).build());
    }
  }
  
}
