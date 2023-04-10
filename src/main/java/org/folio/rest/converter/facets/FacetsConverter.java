package org.folio.rest.converter.facets;

import static org.folio.common.ListUtils.mapItems;

import org.folio.holdingsiq.model.Facets;
import org.folio.holdingsiq.model.PackageFacet;
import org.folio.rest.jaxrs.model.FacetsDto;
import org.folio.rest.jaxrs.model.PackageFacetDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public final class FacetsConverter implements Converter<Facets, FacetsDto> {

  @Autowired
  private Converter<PackageFacet, PackageFacetDto> packageFacetConverter;

  @Override
  public FacetsDto convert(Facets facets) {
    var packages = mapItems(facets.getPackages(), packageFacetConverter::convert);
    return new FacetsDto()
      .withPackages(packages);
  }
}
