package org.folio.rest.validator;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.holdingsiq.model.PackageData;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ResourcePostData;
import org.folio.rest.jaxrs.model.ResourcePostDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePostRequest;
import org.junit.jupiter.api.Test;

class ResourcePostValidatorTest {

  private static final String PACKAGE_ID = "123-456";
  private static final String TITLE_ID = "789";

  private final ResourcePostValidator validator = new ResourcePostValidator();

  @Test
  void shouldValidateRequest() {
    validator.validate(createRequest(PACKAGE_ID, TITLE_ID, "http://example.com"));
  }

  @Test
  void shouldValidateWhenUrlIsNullOrEmpty() {
    validator.validate(createRequest(PACKAGE_ID, TITLE_ID, null));
    validator.validate(createRequest(PACKAGE_ID, TITLE_ID, ""));
  }

  @Test
  void shouldThrowExceptionWhenTitleIdIsNotPresent() {
    var request = createRequest(PACKAGE_ID, null, "http://example.com");
    assertThrows(InputValidationException.class, () -> validator.validate(request));
  }

  @Test
  void shouldThrowExceptionWhenPackageIdIsNotPresent() {
    var request = createRequest(null, TITLE_ID, "http://example.com");
    assertThrows(InputValidationException.class, () -> validator.validate(request));
  }

  @Test
  void shouldThrowExceptionWhenUrlIsInvalid() {
    var request = createRequest(PACKAGE_ID, TITLE_ID, "hdttp://example.com");
    assertThrows(InputValidationException.class, () -> validator.validate(request));
  }

  @Test
  void shouldValidateTitleAndPackage() {
    validator.validateRelatedObjects(createPackage().build(), false);
  }

  @Test
  void shouldThrowExceptionWhenPackageIsNotCustom() {
    PackageData packageData = createPackage()
      .isCustom(false)
      .build();
    assertThrows(InputValidationException.class, () -> validator.validateRelatedObjects(packageData, false));
  }

  @Test
  void shouldThrowExceptionWhenTitleIsAlreadyAddedToPackage() {
    PackageData packageData = createPackage().build();
    assertThrows(InputValidationException.class, () -> validator.validateRelatedObjects(packageData, true));
  }

  private ResourcePostRequest createRequest(String packageId, String titleId, String url) {
    ResourcePostRequest request = new ResourcePostRequest();
    request.setData(new ResourcePostData());
    request.getData().setAttributes(new ResourcePostDataAttributes());
    request.getData().setType(ResourcePostData.Type.RESOURCES);
    request.getData().getAttributes().setPackageId(packageId);
    request.getData().getAttributes().setTitleId(titleId);
    request.getData().getAttributes().setUrl(url);
    return request;
  }

  private PackageData.PackageDataBuilder createPackage() {
    return PackageData.builder()
      .packageName("package")
      .vendorId(123)
      .packageId(456)
      .isCustom(true);
  }
}
