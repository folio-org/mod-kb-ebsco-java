package org.folio.rest.converter.packages;

import static org.folio.common.ListUtils.mapItemsNullable;
import static org.folio.rest.converter.common.ConverterConsts.CONTENT_TYPES;
import static org.folio.rest.converter.packages.PackageConverterUtils.createEmptyPackageRelationship;
import static org.folio.rest.util.RestConstants.PACKAGES_TYPE;

import org.folio.holdingsiq.model.AlternateName;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.Visibility;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackageAltName;
import org.folio.rest.jaxrs.model.PackageCollectionItem;
import org.folio.rest.jaxrs.model.PackageDataAttributes;
import org.folio.rest.jaxrs.model.PackageVisibility;
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
      .withAttributes(convertAttributes(packageData))
      .withRelationships(createEmptyPackageRelationship());
  }

  @SuppressWarnings("checkstyle:MethodLength")
  private PackageDataAttributes convertAttributes(PackageData packageData) {
    return new PackageDataAttributes()
      .withAllowKbToAddTitles(packageData.getAllowEbscoToAddTitles())
      .withContentType(CONTENT_TYPES.get(packageData.getContentType().toLowerCase()))
      .withCustomAltNames(mapItemsNullable(packageData.getCustomAltNames(), this::convertAltName))
      .withCustomCoverage(convertCustomCoverage(packageData))
      .withCustomDescription(packageData.getCustomDescription())
      .withCustomDisplayName(packageData.getCustomDisplayName())
      .withIsAvailableForSelection(packageData.getAvailableForSelection())
      .withIsCustom(packageData.getIsCustom())
      .withIsFreeAccess(packageData.getPackageFreeAccess())
      .withIsPrimaryPackage(packageData.getIsPrimaryPackage())
      .withIsSelected(packageData.getIsSelected())
      .withManagedAltNames(mapItemsNullable(packageData.getManagedAltNames(), this::convertAltName))
      .withManagedDescription(packageData.getManagedDescription())
      .withName(packageData.getPackageName())
      .withPackageId(packageData.getPackageId())
      .withPackageType(packageData.getPackageType())
      .withProviderId(packageData.getVendorId())
      .withProviderName(packageData.getVendorName())
      .withProxiedUrl(packageData.getProxiedUrl())
      .withSelectedCount(packageData.getSelectedCount())
      .withTitleCount(packageData.getTitleCount())
      .withUrl(packageData.getPackageUrl())
      .withVisibility(mapItemsNullable(packageData.getVisibilityDetails(), this::convertVisibility))
      .withVisibilityData(convertVisibilityData(packageData));
  }

  private VisibilityData convertVisibilityData(PackageData packageData) {
    var isHidden = packageData.getVisibilityDetails().stream()
      .map(Visibility::hidden)
      .reduce(Boolean::logicalOr);
    var hiddenByEp = packageData.getVisibilityDetails().stream()
      .map(Visibility::reason)
      .filter("Hidden by EP"::equals)
      .findAny();
    return new VisibilityData().withIsHidden(isHidden.orElse(false))
      .withReason(hiddenByEp.isPresent() ? "Set by system" : "");
  }

  private PackageVisibility convertVisibility(Visibility visibility) {
    return new PackageVisibility()
      .withCategory(PackageVisibility.Category.fromValue(visibility.category().toUpperCase()))
      .withHidden(visibility.hidden())
      .withReason(visibility.reason());
  }

  private Coverage convertCustomCoverage(PackageData packageData) {
    return new Coverage()
      .withBeginCoverage(packageData.getCustomCoverage().getBeginCoverage())
      .withEndCoverage(packageData.getCustomCoverage().getEndCoverage());
  }

  private PackageAltName convertAltName(AlternateName alternateName) {
    return new PackageAltName()
      .withId(alternateName.id() == null ? null : alternateName.id())
      .withAltName(alternateName.altName());
  }
}
