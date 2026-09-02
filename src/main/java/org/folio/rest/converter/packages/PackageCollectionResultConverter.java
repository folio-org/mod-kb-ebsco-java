package org.folio.rest.converter.packages;

import static org.folio.common.ListUtils.mapItems;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.PackageId;
import org.folio.repository.accesstypes.DbAccessType;
import org.folio.repository.packages.DbPackage;
import org.folio.rest.jaxrs.model.AccessType;
import org.folio.rest.jaxrs.model.HasOneRelationship;
import org.folio.rest.jaxrs.model.MetaDataIncluded;
import org.folio.rest.jaxrs.model.MetaTotalResults;
import org.folio.rest.jaxrs.model.PackageCollection;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.RelationshipData;
import org.folio.rest.jaxrs.model.Tags;
import org.folio.rest.util.RestConstants;
import org.folio.rmapi.result.PackageCollectionResult;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PackageCollectionResultConverter implements Converter<PackageCollectionResult, PackageCollection> {

  private final Converter<PackageData, PackageCollectionItem> packageCollectionItemConverter;
  private final Converter<DbAccessType, AccessType> accessTypeConverter;

  public PackageCollectionResultConverter(
    Converter<PackageData, PackageCollectionItem> packageCollectionItemConverter,
    Converter<DbAccessType, AccessType> accessTypeConverter) {
    this.packageCollectionItemConverter = packageCollectionItemConverter;
    this.accessTypeConverter = accessTypeConverter;
  }

  @Override
  public PackageCollection convert(PackageCollectionResult packagesResult) {
    var packages = packagesResult.packages();
    var dbPackages = packagesResult.dbPackages();
    var accessTypes = packagesResult.accessTypes();
    var packageList = mapItems(packages.getPackagesList(),
      packageData -> {
        var item = packageCollectionItemConverter.convert(packageData);
        item.getAttributes()
          .withTags(new Tags()
            .withTagList(getTagsById(dbPackages, createPackageId(packageData))));
        addAccessType(item, packageData.getFullPackageId(), accessTypes);
        return item;
      });

    return new PackageCollection()
      .withJsonapi(RestConstants.JSONAPI)
      .withMeta(new MetaTotalResults().withTotalResults(packages.getTotalResults()))
      .withData(packageList);
  }

  private void addAccessType(PackageCollectionItem item, String packageFullId,
                             Map<String, DbAccessType> accessTypes) {
    var dbAccessType = accessTypes.get(packageFullId);
    if (dbAccessType == null) {
      return;
    }
    var accessType = accessTypeConverter.convert(dbAccessType);
    item.getIncluded().add(accessType);
    item.getRelationships()
      .withAccessType(new HasOneRelationship()
        .withData(new RelationshipData()
          .withId(accessType.getId())
          .withType(AccessType.Type.ACCESS_TYPES.value()))
        .withMeta(new MetaDataIncluded()
          .withIncluded(true)));
  }

  private List<String> getTagsById(List<DbPackage> packages, PackageId packageId) {
    return packages.stream()
      .filter(dbPackage -> dbPackage.getId().equals(packageId))
      .map(DbPackage::getTags)
      .findFirst()
      .orElse(Collections.emptyList());
  }

  private PackageId createPackageId(PackageData packageData) {
    return new PackageId(packageData.getVendorId(), packageData.getPackageId());
  }
}
