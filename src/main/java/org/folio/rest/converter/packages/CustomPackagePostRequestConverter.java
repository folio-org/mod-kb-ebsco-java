package org.folio.rest.converter.packages;

import static org.folio.common.ListUtils.mapItemsNullable;
import static org.folio.rest.converter.packages.PackageConverterUtils.CONTENT_TYPE_TO_RMAPI_CODE;

import javax.annotation.Nullable;
import org.folio.holdingsiq.model.AlternateName;
import org.folio.holdingsiq.model.CoverageDates;
import org.folio.holdingsiq.model.PackagePost;
import org.folio.holdingsiq.model.Proxy;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackageAltName;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.ProxyDto;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CustomPackagePostRequestConverter implements Converter<PackagePostRequest, PackagePost> {

  @Override
  public PackagePost convert(PackagePostRequest postPackageBody) {
    var data = postPackageBody.getData();
    var attributes = data.getAttributes();
    return PackagePost.builder()
      .customAltNames(mapItemsNullable(attributes.getCustomAltNames(), this::convertAlternateName))
      .coverage(convertCoverageDates(attributes.getCustomCoverage()))
      .customDescription(attributes.getCustomDescription())
      .customDisplayName(attributes.getCustomDisplayName())
      .contentType(CONTENT_TYPE_TO_RMAPI_CODE.getOrDefault(attributes.getContentType(), 6))
      .packageName(attributes.getName())
      .packageFreeAccess(attributes.getIsFreeAccess())
      .proxy(convertProxy(attributes.getProxy()))
      .build();
  }

  private @Nullable CoverageDates convertCoverageDates(@Nullable Coverage customCoverage) {
    if (customCoverage == null) {
      return null;
    }
    return CoverageDates.builder()
      .beginCoverage(customCoverage.getBeginCoverage())
      .endCoverage(customCoverage.getEndCoverage())
      .build();
  }

  private @Nullable Proxy convertProxy(@Nullable ProxyDto proxy) {
    if (proxy == null) {
      return null;
    }
    return Proxy.builder()
      .id(proxy.getId())
      .inherited(false)
      .build();
  }

  private @Nullable AlternateName convertAlternateName(@Nullable PackageAltName packageAltName) {
    if (packageAltName == null) {
      return null;
    }
    return new AlternateName(packageAltName.getId(), packageAltName.getAltName());
  }
}
