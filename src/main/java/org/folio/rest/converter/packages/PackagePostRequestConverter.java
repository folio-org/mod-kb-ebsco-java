package org.folio.rest.converter.packages;

import static org.folio.rest.converter.packages.PackageConverterUtils.contentTypeToRMAPICode;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.Coverage;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rmapi.model.PackagePost;

@Component
public class PackagePostRequestConverter implements Converter<PackagePostRequest, PackagePost> {

  @Override
  public PackagePost convert(@NonNull PackagePostRequest postPackageBody) {
    PackagePost.PackagePostBuilder postRequest = PackagePost.builder()
      .contentType(contentTypeToRMAPICode.getOrDefault(postPackageBody.getData().getAttributes().getContentType(), 6))
      .packageName(postPackageBody.getData().getAttributes().getName());

    Coverage customCoverage = postPackageBody.getData().getAttributes().getCustomCoverage();
    if (customCoverage != null) {
      postRequest.coverage(
        org.folio.rmapi.model.CoverageDates.builder()
          .beginCoverage(customCoverage.getBeginCoverage())
          .endCoverage(customCoverage.getEndCoverage())
          .build());
    }

    return postRequest.build();
  }

}
