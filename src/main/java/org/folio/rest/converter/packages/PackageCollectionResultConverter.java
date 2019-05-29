package org.folio.rest.converter.packages;

import static org.folio.common.ListUtils.mapItems;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.PackageId;
import org.folio.holdingsiq.model.Packages;
import org.folio.repository.packages.DbPackage;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.PackageCollectionResult;

@Component
public class PackageCollectionResultConverter implements Converter<PackageCollectionResult, PackageCollection> {

  @Autowired
  private Converter<PackageData, PackageCollectionItem> packageCollectionItemConverter;

  @Override
  public PackageCollection convert(@NonNull PackageCollectionResult packagesResult) {
    Packages packages = packagesResult.getPackages();
    List<DbPackage> dbPackages = packagesResult.getDbPackages();
    List<PackageCollectionItem> packageList = mapItems(packages.getPackagesList(),
      packageData -> {
        PackageCollectionItem item = packageCollectionItemConverter.convert(packageData);
        item.getAttributes()
          .withTags(new Tags()
            .withTagList(getTagsById(dbPackages, createPackageId(packageData))));
        return item;
      });

    return new PackageCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(packages.getTotalResults()))
      .withData(packageList);
  }

  private List<String> getTagsById(List<DbPackage> packages, PackageId packageId) {
    return packages.stream()
      .filter(dbPackage -> dbPackage.getId().equals(packageId))
      .map(DbPackage::getTags)
      .findFirst()
      .orElse(Collections.emptyList());
  }

  private PackageId createPackageId(PackageData packageData) {
    return PackageId.builder().providerIdPart(packageData.getVendorId()).packageIdPart(packageData.getPackageId()).build();
  }
}
