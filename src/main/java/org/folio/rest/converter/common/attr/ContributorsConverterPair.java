package org.folio.rest.converter.common.attr;

import static org.folio.common.ListUtils.mapItems;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.folio.holdingsiq.model.Contributor;
import org.folio.rest.jaxrs.model.Contributors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

public final class ContributorsConverterPair {

  private ContributorsConverterPair() {
  }

  @Component
  public static class FromRmApi implements Converter<List<Contributor>, List<Contributors>> {

    @Override
    public List<@NonNull Contributors> convert(@Nullable List<Contributor> contributorList) {
      if (Objects.isNull(contributorList)) {
        return Collections.emptyList();
      }
      return mapItems(contributorList,
        contributor -> new Contributors()
          .withContributor(contributor.getTitleContributor())
          .withType(contributor.getType()));
    }
  }

  @Component
  public static class ToRmApi implements Converter<List<Contributors>, List<Contributor>> {

    @Override
    public List<@NonNull Contributor> convert(List<Contributors> contributorList) {
      return mapItems(contributorList,
        contributor -> org.folio.holdingsiq.model.Contributor.builder()
          .titleContributor(contributor.getContributor())
          .type(contributor.getType()).build());
    }
  }
}
