package org.folio.rest.validator.resources;

import lombok.RequiredArgsConstructor;
import org.folio.holdingsiq.model.PackageData;
import org.folio.rest.jaxrs.model.ResourcePostRequest;
import org.folio.rest.jaxrs.model.ResourcePutRequest;
import org.folio.rest.jaxrs.model.ResourceTagsDataAttributes;
import org.folio.rest.jaxrs.model.ResourceTagsPutRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceValidationService {

  private final ResourcePostValidator resourcePostValidator;
  private final ManagedResourcePutBodyValidator managedResourcePutBodyValidator;
  private final CustomResourcePutBodyValidator customResourcePutBodyValidator;
  private final ResourceTagsPutBodyValidator resourceTagsPutBodyValidator;

  public void validateResourcePostRequest(ResourcePostRequest request) {
    resourcePostValidator.validate(request);
  }

  public void validateResourcePostRelatedObjects(PackageData packageData, boolean titleInPackage) {
    resourcePostValidator.validateRelatedObjects(packageData, titleInPackage);
  }

  public void validateManagedResourcePutRequest(ResourcePutRequest request) {
    managedResourcePutBodyValidator.validate(request);
  }

  public void validateCustomResourcePutRequest(ResourcePutRequest request) {
    customResourcePutBodyValidator.validate(request);
  }

  public void validateResourceTagsPutRequest(ResourceTagsPutRequest request, ResourceTagsDataAttributes attributes) {
    resourceTagsPutBodyValidator.validate(request, attributes);
  }
}
