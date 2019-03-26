package org.folio.rest.converter.packages;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.Packages;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.util.RestConstants;

@Component
public class PackageCollectionConverter implements Converter<Packages, PackageCollection> {

  @Autowired
  private Converter<PackageData, PackageCollectionItem> packageCollectionItemConverter;

  @Override
  public PackageCollection convert(@NonNull Packages packages) {
    List<PackageCollectionItem> packageList = mapItems(packages.getPackagesList(),
      packageData -> packageCollectionItemConverter.convert(packageData));
    
    return new PackageCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(packages.getTotalResults()))
      .withData(packageList);
  }
}
