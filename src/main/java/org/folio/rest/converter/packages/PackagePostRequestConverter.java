package org.folio.rest.converter.packages;

import static org.folio.rest.converter.packages.PackageConverterUtils.CONTENT_TYPE_TO_RMAPI_CODE;

import org.folio.holdingsiq.model.PackagePost;
import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class PackagePostRequestConverter implements Converter<PackagePostRequest, PackagePost> {

  @Override
  public PackagePost convert(@NonNull PackagePostRequest postPackageBody) {
    var data = postPackageBody.getData();
    var attributes = data.getAttributes();
    PackagePost.PackagePostBuilder postRequest = PackagePost.builder()
      .contentType(CONTENT_TYPE_TO_RMAPI_CODE.getOrDefault(attributes.getContentType(), 6))
      .packageName(attributes.getName());

    Coverage customCoverage = attributes.getCustomCoverage();
    if (customCoverage != null) {
      postRequest.coverage(
        org.folio.holdingsiq.model.CoverageDates.builder()
          .beginCoverage(customCoverage.getBeginCoverage())
          .endCoverage(customCoverage.getEndCoverage())
          .build());
    }

    return postRequest.build();
  }

}
