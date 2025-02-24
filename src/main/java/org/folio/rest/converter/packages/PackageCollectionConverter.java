package org.folio.rest.converter.packages;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.Packages;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.util.RestConstants;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class PackageCollectionConverter implements Converter<Packages, PackageCollection> {

  private final Converter<PackageData, PackageCollectionItem> packageCollectionItemConverter;

  public PackageCollectionConverter(Converter<PackageData, PackageCollectionItem> packageCollectionItemConverter) {
    this.packageCollectionItemConverter = packageCollectionItemConverter;
  }

  @Override
  public PackageCollection convert(@NonNull Packages packages) {
    List<PackageCollectionItem> packageList = mapItems(packages.getPackagesList(),
      packageCollectionItemConverter::convert);

    return new PackageCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(packages.getTotalResults()))
      .withData(packageList);
  }
}
