package org.folio.rest.validator.packages;

import lombok.RequiredArgsConstructor;
import org.folio.rest.jaxrs.model.PackagePostRequest;
import org.folio.rest.jaxrs.model.PackagePutRequest;
import org.folio.rest.jaxrs.model.PackageTagsPutRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PackageValidationService {

  private final ManagedPackagePutBodyValidator managedPackagePutBodyValidator;
  private final CustomPackagePutBodyValidator customPackagePutBodyValidator;
  private final PackageTagsPutBodyValidator packageTagsPutBodyValidator;
  private final PackagesPostBodyValidator packagesPostBodyValidator;

  public void validateManagedPackagePutRequest(PackagePutRequest request) {
    managedPackagePutBodyValidator.validate(request);
  }

  public void validateCustomPackagePutRequest(PackagePutRequest request) {
    customPackagePutBodyValidator.validate(request);
  }

  public void validateCustomPackagePostRequest(PackagePostRequest request) {
    packagesPostBodyValidator.validate(request);
  }

  public void validatePackageTagsPutRequest(PackageTagsPutRequest request) {
    packageTagsPutBodyValidator.validate(request);
  }
}
