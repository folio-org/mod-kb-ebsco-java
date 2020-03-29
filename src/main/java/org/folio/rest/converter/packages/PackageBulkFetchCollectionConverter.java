package org.folio.rest.converter.packages;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.PackageData;
import org.folio.rest.jaxrs.model.FailedPackageIds;
import org.folio.rest.jaxrs.model.FailedPackagesInformation;
import org.folio.rest.jaxrs.model.PackageBulkFetchCollection;
import org.folio.rest.jaxrs.model.PackageBulkFetchCollectionItem;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.PackageBulkResult;

@Component
public class PackageBulkFetchCollectionConverter implements Converter<PackageBulkResult, PackageBulkFetchCollection> {

  @Autowired
  private Converter<PackageData, PackageBulkFetchCollectionItem> packageBulkItemConverter;

  @Override
  public PackageBulkFetchCollection convert(@NotNull PackageBulkResult source) {
    List<PackageBulkFetchCollectionItem> packageList = mapItems(source.getPackages().getPackagesList(),
        packageBulkItemConverter::convert);

    return new PackageBulkFetchCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withIncluded(packageList)
      .withMeta(toMeta(source.getFailedPackageIds()));
  }

  private FailedPackagesInformation toMeta(List<String> failedPackageIds) {
    return new FailedPackagesInformation()
      .withFailed(new FailedPackageIds().withPackages(failedPackageIds));
  }

}
