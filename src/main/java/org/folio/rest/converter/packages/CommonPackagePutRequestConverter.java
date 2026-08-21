package org.folio.rest.converter.packages;

import static org.folio.common.ListUtils.mapItems;

import java.util.List;
import org.folio.holdingsiq.model.AlternateName;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.PackagePut;
import org.folio.holdingsiq.model.Proxy;
import org.folio.holdingsiq.model.Visibility;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.PackageVisibility;

public abstract class CommonPackagePutRequestConverter {

  static final String HIDDEN_BY_CUSTOMER = "Hidden by Customer";

  protected PackagePut.PackagePutBuilder convertCommonAttributes(PackagePutDataAttributes attributes) {
    var builder = PackagePut.builder();

    convertSimpleFields(attributes, builder);

    if (attributes.getProxy() != null) {
      builder.proxy(convertProxy(attributes));
    }

    if (attributes.getCustomCoverage() != null) {
      builder.customCoverage(convertCoverageDates(attributes));
    }

    if (attributes.getCustomAltNames() != null) {
      builder.customAltNames(convertCustomAltNames(attributes));
    }

    builder.visibilityDetails(mapItems(attributes.getVisibility(), this::convertVisibility));

    return builder;
  }

  protected Visibility convertVisibility(PackageVisibility pv) {
    var reason = Boolean.TRUE.equals(pv.getHidden()) ? HIDDEN_BY_CUSTOMER : null;
    return new Visibility(pv.getCategory().value(), pv.getHidden(), reason);
  }

  private void convertSimpleFields(PackagePutDataAttributes attributes, PackagePut.PackagePutBuilder builder) {
    builder.isSelected(attributes.getIsSelected());
    builder.isFullPackage(attributes.getIsFullPackage());
    builder.customDescription(attributes.getCustomDescription());
    builder.customDisplayName(attributes.getCustomDisplayName());
    builder.packageFreeAccess(attributes.getIsFreeAccess());
    builder.packageUrl(attributes.getUrl());
  }

  private List<AlternateName> convertCustomAltNames(PackagePutDataAttributes attributes) {
    return mapItems(attributes.getCustomAltNames(),
      packageAltName -> new AlternateName(packageAltName.getId(), packageAltName.getAltName()));
  }

  private CoverageDates convertCoverageDates(PackagePutDataAttributes attributes) {
    return CoverageDates.builder()
      .beginCoverage(attributes.getCustomCoverage().getBeginCoverage())
      .endCoverage(attributes.getCustomCoverage().getEndCoverage())
      .build();
  }

  private Proxy convertProxy(PackagePutDataAttributes attributes) {
    return Proxy.builder()
      .id(attributes.getProxy().getId())
      // RM API gives an error when we pass inherited as true along with updated proxy value
      // Hard code it to false; it should not affect the state of inherited that RM API maintains
      .inherited(false)
      .build();
  }
}
