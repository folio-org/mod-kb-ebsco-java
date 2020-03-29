package org.folio.rest.converter.packages;

import static org.folio.rest.converter.common.ConverterConsts.contentTypes;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.holdingsiq.model.PackageData;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackageBulkFetchCollectionItem;
import org.folio.rest.jaxrs.model.PackageBulkFetchDataAttributes;

@Component
public class PackageBulkFetchCollectionItemConverter implements Converter<PackageData, PackageBulkFetchCollectionItem> {

  @Override
  public PackageBulkFetchCollectionItem convert(@NonNull PackageData packageData) {
    Integer providerId = packageData.getVendorId();
    String providerName = packageData.getVendorName();
    Integer packageId = packageData.getPackageId();

    return new PackageBulkFetchCollectionItem()
      .withId(providerId + "-" + packageId)
      .withType(PackageBulkFetchCollectionItem.Type.PACKAGES)
      .withAttributes(new PackageBulkFetchDataAttributes()
        .withContentType(contentTypes.get(packageData.getContentType().toLowerCase()))
        .withCustomCoverage(
          new Coverage()
            .withBeginCoverage(packageData.getCustomCoverage().getBeginCoverage())
            .withEndCoverage(packageData.getCustomCoverage().getEndCoverage())
        )
        .withIsCustom(packageData.getIsCustom())
        .withIsSelected(packageData.getIsSelected())
        .withName(packageData.getPackageName())
        .withPackageId(packageId)
        .withPackageType(packageData.getPackageType())
        .withProviderId(providerId)
        .withProviderName(providerName)
        .withSelectedCount(packageData.getSelectedCount())
        .withTitleCount(packageData.getTitleCount())
      );
  }
}
