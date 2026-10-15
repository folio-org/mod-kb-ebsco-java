package org.folio.rest.converter.packages;

import static java.util.Objects.isNull;
import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import org.folio.holdingsiq.model.Facets;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.Packages;
import org.folio.rest.jaxrs.model.FacetsDto;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.util.RestConstants;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PackageCollectionConverter implements Converter<Packages, PackageCollection> {

  private final Converter<PackageData, PackageCollectionItem> packageCollectionItemConverter;
  private final Converter<Facets, FacetsDto> facetsConverter;

  public PackageCollectionConverter(Converter<PackageData, PackageCollectionItem> packageCollectionItemConverter,
                                    Converter<Facets, FacetsDto> facetsConverter) {
    this.packageCollectionItemConverter = packageCollectionItemConverter;
    this.facetsConverter = facetsConverter;
  }

  @Override
  public PackageCollection convert(Packages packages) {
    List<PackageCollectionItem> packageList = mapItems(packages.getPackagesList(),
      packageCollectionItemConverter::convert);
    var facetsDto = isNull(packages.getFacets()) ? null : facetsConverter.convert(packages.getFacets());

    return new PackageCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withFacets(facetsDto)
      .withMeta(new MetaTotalResults().withTotalResults(packages.getTotalResults()))
      .withData(packageList);
  }
}
