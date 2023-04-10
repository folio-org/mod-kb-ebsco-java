package org.folio.rest.converter.facets;

import org.folio.holdingsiq.model.PackageFacet;
import org.folio.rest.jaxrs.model.PackageFacetDto;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public final class PackageFacetConverter implements Converter<PackageFacet, PackageFacetDto> {

  @Override
  public PackageFacetDto convert(@NonNull PackageFacet packageFacet) {
    return new PackageFacetDto()
      .withId(packageFacet.getPackageId())
      .withName(packageFacet.getPackageName())
      .withCount(packageFacet.getTotalCount());
  }
}
