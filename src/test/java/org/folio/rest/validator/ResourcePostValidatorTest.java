package org.folio.rest.validator;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import org.folio.holdingsiq.model.PackageData;
import org.folio.holdingsiq.model.Title;
import org.folio.holdingsiq.model.Titles;
import org.folio.rest.exception.InputValidationException;
import org.folio.rest.jaxrs.model.ResourcePostData;
import org.folio.rest.jaxrs.model.ResourcePostDataAttributes;
import org.folio.rest.jaxrs.model.ResourcePostRequest;
import org.junit.jupiter.api.Test;

class ResourcePostValidatorTest {

  private static final String PACKAGE_ID = "123-456";
  private static final String TITLE_ID = "789";
  private static final String TITLE_NAME = "title name";
  private static final int OTHER_TITLE_ID = 555;

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
    validator.validateRelatedObjects(
      createPackage().build(),
      createTitle().build(),
      createTitles().build());
  }

  @Test
  void shouldThrowExceptionWhenPackageIsNotCustom() {
    PackageData packageData = createPackage()
      .isCustom(false)
      .build();
    Title build = createTitle().build();
    Titles build1 = createTitles().build();
    assertThrows(InputValidationException.class, () ->
      validator.validateRelatedObjects(
        packageData,
        build,
        build1));
  }

  @Test
  void shouldThrowExceptionWhenTitleIsAlreadyAddedToPackage() {
    Title title = createTitle().build();
    Titles titles = createTitles().titleList(Collections.singletonList(title)).build();
    PackageData packageData = createPackage().build();
    assertThrows(InputValidationException.class, () ->
      validator.validateRelatedObjects(
        packageData,
        title,
        titles));
  }

  private Titles.TitlesBuilder createTitles() {
    List<Title> titles = Collections.singletonList(createTitle().titleId(OTHER_TITLE_ID).build());
    return Titles.builder().titleList(titles);
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

  private Title.TitleBuilder createTitle() {
    return Title.builder()
      .titleId(Integer.valueOf(TITLE_ID))
      .titleName(TITLE_NAME);
  }
}
