package org.folio.rest.converter.packages;

import static org.folio.rest.converter.common.ConverterConsts.CONTENT_TYPES;
import static org.folio.rest.converter.packages.PackageConverterUtils.createEmptyPackageRelationship;
import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;

import org.folio.holdingsiq.model.PackageData;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.VisibilityData;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PackageCollectionItemConverter implements Converter<PackageData, PackageCollectionItem> {

  @Override
  public PackageCollectionItem convert(PackageData packageData) {
    return new PackageCollectionItem()
      .withId(packageData.getVendorId() + "-" + packageData.getPackageId())
      .withType(PACKAGES_TYPE)
      .withAttributes(new PackageDataAttributes()
        .withContentType(CONTENT_TYPES.get(packageData.getContentType().toLowerCase()))
        .withCustomCoverage(convertCustomCoverage(packageData))
        .withIsCustom(packageData.getIsCustom())
        .withIsSelected(packageData.getIsSelected())
        .withName(packageData.getPackageName())
        .withPackageId(packageData.getPackageId())
        .withPackageType(packageData.getPackageType())
        .withProviderId(packageData.getVendorId())
        .withProviderName(packageData.getVendorName())
        .withSelectedCount(packageData.getSelectedCount())
        .withTitleCount(packageData.getTitleCount())
        .withAllowKbToAddTitles(packageData.getAllowEbscoToAddTitles())
        .withVisibilityData(convertVisibilityData(packageData)))
      .withRelationships(createEmptyPackageRelationship());
  }

  private VisibilityData convertVisibilityData(PackageData packageData) {
    return new VisibilityData().withIsHidden(packageData.getVisibilityData().getIsHidden())
      .withReason(packageData.getVisibilityData().getReason().equals("Hidden by EP") ? "Set by system" : "");
  }

  private Coverage convertCustomCoverage(PackageData packageData) {
    return new Coverage()
      .withBeginCoverage(packageData.getCustomCoverage().getBeginCoverage())
      .withEndCoverage(packageData.getCustomCoverage().getEndCoverage());
  }
}
