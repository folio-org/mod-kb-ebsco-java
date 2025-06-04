package org.folio.rest.converter.packages;

import static org.folio.rest.converter.packages.PackageConverterUtils.CONTENT_TYPE_TO_RMAPI_CODE;

import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.PackagePut;
import org.folio.holdingsiq.model.TokenInfo;
import org.folio.rest.jaxrs.model.PackagePutDataAttributes;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.springframework.stereotype.Component;

@Component
public class PackageRequestConverter {

  public PackagePut convertToRmApiCustomPackagePutRequest(PackagePutRequest request) {
    PackagePutDataAttributes attributes = request.getData().getAttributes();
    PackagePut.PackagePutBuilder builder = convertCommonAttributesToPackagePutRequest(attributes);
    builder.packageName(attributes.getName());
    Integer contentType = CONTENT_TYPE_TO_RMAPI_CODE.get(attributes.getContentType());
    builder.contentType(contentType != null ? contentType : 6);
    return builder.build();
  }

  public PackagePut convertToRmApiPackagePutRequest(PackagePutRequest request) {
    PackagePutDataAttributes attributes = request.getData().getAttributes();
    PackagePut.PackagePutBuilder builder = convertCommonAttributesToPackagePutRequest(attributes);
    builder.allowEbscoToAddTitles(attributes.getAllowKbToAddTitles());
    if (attributes.getPackageToken() != null) {
      TokenInfo tokenInfo = TokenInfo.builder()
        .value(attributes.getPackageToken().getValue())
        .build();
      builder.packageToken(tokenInfo);
    }
    return builder.build();
  }

  private PackagePut.PackagePutBuilder convertCommonAttributesToPackagePutRequest(PackagePutDataAttributes attributes) {
    PackagePut.PackagePutBuilder builder = PackagePut.builder();

    builder.isSelected(attributes.getIsSelected());
    builder.isFullPackage(attributes.getIsFullPackage());

    if (attributes.getProxy() != null) {
      org.folio.holdingsiq.model.Proxy proxy = org.folio.holdingsiq.model.Proxy.builder()
        .id(attributes.getProxy().getId())
        // RM API gives an error when we pass inherited as true along with updated proxy value
        // Hard code it to false; it should not affect the state of inherited that RM API maintains
        .inherited(false)
        .build();
      builder.proxy(proxy);
    }

    if (attributes.getVisibilityData() != null) {
      builder.isHidden(attributes.getVisibilityData().getIsHidden());
    }

    if (attributes.getCustomCoverage() != null) {
      CoverageDates coverageDates = CoverageDates.builder()
        .beginCoverage(attributes.getCustomCoverage().getBeginCoverage())
        .endCoverage(attributes.getCustomCoverage().getEndCoverage())
        .build();
      builder.customCoverage(coverageDates);
    }

    return builder;
  }
}
