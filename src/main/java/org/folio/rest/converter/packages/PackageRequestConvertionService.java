package org.folio.rest.converter.packages;

import lombok.RequiredArgsConstructor;
import org.folio.holdingsiq.model.PackagePost;
import org.folio.holdingsiq.model.PackagePut;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PackageRequestConvertionService {

  private final ManagedPackagePutRequestConverter managedPackagePutRequestConverter;
  private final CustomPackagePutRequestConverter customPackagePutRequestConverter;
  private final CustomPackagePostRequestConverter customPackagePostRequestConverter;

  public PackagePut convertCustomPackagePutRequest(PackagePutRequest request) {
    return customPackagePutRequestConverter.convert(request);
  }

  public PackagePut convertManagedPackagePutRequest(PackagePutRequest request) {
    return managedPackagePutRequestConverter.convert(request);
  }

  public PackagePost convertCustomPackagePostRequest(PackagePostRequest request) {
    return customPackagePostRequestConverter.convert(request);
  }
}
