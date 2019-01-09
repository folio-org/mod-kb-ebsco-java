package org.folio.rest.converter.packages;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.model.PackageData;
import org.folio.rmapi.model.Packages;

@Component
public class PackageCollectionConverter implements Converter<Packages, PackageCollection> {

  @Autowired
  private Converter<PackageData, PackageCollectionItem> packageCollectionItemConverter;

  @Override
  public PackageCollection convert(@NonNull Packages packages) {
    List<PackageCollectionItem> packageList = packages.getPackagesList().stream()
      .map(packageData -> packageCollectionItemConverter.convert(packageData))
      .collect(Collectors.toList());
    return new PackageCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(packages.getTotalResults()))
      .withData(packageList);
  }
}
