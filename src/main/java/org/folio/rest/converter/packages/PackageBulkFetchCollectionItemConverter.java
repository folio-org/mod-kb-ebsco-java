package org.folio.rest.converter.packages;

import static org.folio.rest.converter.common.ConverterConsts.CONTENT_TYPES;

import org.folio.holdingsiq.model.PackageData;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackageBulkFetchCollectionItem;
import org.folio.rest.jaxrs.model.PackageBulkFetchDataAttributes;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PackageBulkFetchCollectionItemConverter implements Converter<PackageData, PackageBulkFetchCollectionItem> {

  @Override
  public PackageBulkFetchCollectionItem convert(PackageData packageData) {
    Integer providerId = packageData.getVendorId();
    String providerName = packageData.getVendorName();
    Integer packageId = packageData.getPackageId();

    return new PackageBulkFetchCollectionItem()
      .withId(providerId + "-" + packageId)
      .withType(PackageBulkFetchCollectionItem.Type.PACKAGES)
      .withAttributes(new PackageBulkFetchDataAttributes()
        .withContentType(CONTENT_TYPES.get(packageData.getContentType().toLowerCase()))
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
